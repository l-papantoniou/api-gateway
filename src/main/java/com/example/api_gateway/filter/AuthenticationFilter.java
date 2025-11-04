package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {


    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Get Authorization header
            String headerName = config.getHeaderName();
            if (!request.getHeaders().containsKey(headerName)) {
                log.warn("Missing {} header for: {}", headerName, path);
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(headerName);

            // Validate header format
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid {} header format for: {}", headerName, path);
                return onError(exchange, "Invalid authorization format", HttpStatus.UNAUTHORIZED);
            }

            // Extract token
            String token = authHeader.substring(7);

            // Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired token for: {}", path);
                return onError(exchange, "Invalid or expired token", HttpStatus.FORBIDDEN);
            }

            // Extract user information from token
            String userId = jwtUtil.getUserId(token);
            String userRole = jwtUtil.getUserRole(token);
            String userEmail = jwtUtil.getUserEmail(token);

            // Add user info to request headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", userRole)
                    .header("X-User-Email", userEmail)
                    .header("X-Authenticated", "true")
                    .build();

            log.debug("Authenticated user: {} for path: {}", userId, path);

            // Continue with modified request
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        });
    }


    /**
     * Returns error response with JSON body
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-type", "application/json");

        String errorResponse = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
                message,
                status.value(),
                java.time.Instant.now().toString()
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    /**
     * Configuration class for AuthenticationFilter
     */
    @Getter
    @Setter
    public static class Config {
        private String headerName = "Authorization";
    }
}
