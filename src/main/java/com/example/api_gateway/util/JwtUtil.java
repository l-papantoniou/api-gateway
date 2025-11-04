package com.example.api_gateway.util;

import com.example.api_gateway.config.JwtProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {

    private final RSAPublicKey publicKey;
    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {

        this.jwtProperties = jwtProperties;

        try {
            this.publicKey = loadPublicKeyFromJwks(jwtProperties.getJwksUri());
            log.info("Successfully loaded public key from JWKS endpoint: {}", jwtProperties.getJwksUri());
            log.info("Issuer configured: {}", jwtProperties.getIssuer());
        } catch (Exception e) {
            log.error("Failed to load public key", e);
            throw new RuntimeException("Cannot initialize JWT validation", e);
        }
    }


    /**
     * Load public key from JWKS endpoint
     */
    private RSAPublicKey loadPublicKeyFromJwks(String jwksUri) throws Exception {
        log.info("Fetching JWKS from: {}", jwksUri);

        // Fetch JWKS JSON from authorization server
        WebClient webClient = WebClient.create();
        String jwksJson = webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // block since we need it at startup

        if (jwksJson == null || jwksJson.isEmpty()) {
            throw new RuntimeException("Empty JWKS response from: " + jwksUri);
        }

        log.debug("JWKS response: {}", jwksJson);

        // Parse JWKS JSON
        JWKSet jwkSet = JWKSet.parse(jwksJson);

        if (jwkSet.getKeys().isEmpty()) {
            throw new RuntimeException("No keys found in JWKS");
        }

        // Get the first RSA key
        // If we have multiple keys we can match by "kid" (key id)
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().getFirst();

        log.info("Loaded RSA key with kid: {}", rsaKey.getKeyID());

        // Convert to Java RSA public key
        return rsaKey.toRSAPublicKey();

    }

    /**
     * Validates JWT token OFFLINE using local public key
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)  // Verify with public key from JWKS
                    .requireIssuer(jwtProperties.getIssuer())   // Validate issuer
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check expiration
            if (claims.getExpiration().before(new Date())) {
                log.warn("Token expired at: {}", claims.getExpiration());
                return false;
            }

            log.debug("Token validated successfully for user: {}", claims.getSubject());
            return true;

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Extracts user ID from token
     */
    public String getUserId(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }


    /**
     * Extract user roles from token
     */
    public String getUserRole(String token) {
        Claims claims = getClaims(token);
        if (claims == null) return null;

        // Try different claim names (depends on your auth server)
        Object role = claims.get("role");
        if (role == null) role = claims.get("roles");
        if (role == null) role = claims.get("authorities");

        return role != null ? role.toString() : null;
    }

    /**
     * Extracts custom claims from token
     */
    public String getUserEmail(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("email", String.class) : null;
    }


    /**
     * Get all claims from token
     */
    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("Cannot parse token claims: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Claims claims = getClaims(token);
        return claims == null || claims.getExpiration().before(new Date());
    }


    /**
     * Loads public key from file
     */
    private PublicKey loadPublicKeyFromFile(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String keyContent = reader.lines()
                    .filter(line -> !line.contains("BEGIN") && !line.contains("END"))
                    .collect(Collectors.joining());

            return parsePublicKey(keyContent);
        }

    }

    /**
     * Parses base64 encoded public key
     */
    private PublicKey parsePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }


}

