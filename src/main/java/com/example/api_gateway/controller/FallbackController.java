package com.example.api_gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/products")
    public Mono<ResponseEntity<Map<String, Object>>> productFallback() {
        log.warn("Product service fallback triggered");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product service is temporarily unavailable");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", LocalDateTime.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback() {
        log.warn("User service fallback triggered");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User service is temporarily unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", LocalDateTime.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
