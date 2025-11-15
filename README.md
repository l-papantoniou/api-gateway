# API Gateway

A production-ready Spring Cloud Gateway implementation that serves as a secure, centralized entry point for microservices architecture. Built with Spring WebFlux for reactive, non-blocking request handling.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

---

## 🎯 Overview

This API Gateway acts as a single entry point for all client requests to backend microservices. It handles cross-cutting concerns such as authentication, routing, logging, and security, allowing downstream services to focus on business logic.

### Key Capabilities

- **Authentication**: JWT token validation with offline verification using RSA public keys
- **Authorization**: Extracts user context and propagates to downstream services
- **Routing**: Intelligent path-based routing to multiple microservices
- **Observability**: Request/response logging with performance metrics
- **Resilience**: Circuit breaker and retry mechanisms (configurable)
- **Security**: Centralized security enforcement

---

## ✨ Features

### 🔐 Security
- **JWT Token Validation**: Validates tokens offline using public keys from JWKS endpoint
- **Bearer Token Authentication**: Supports standard OAuth 2.0 Bearer token format
- **User Context Propagation**: Extracts and forwards user information (ID, role, email) as HTTP headers

### 🚦 Routing
- **Path-Based Routing**: Routes requests based on URL patterns
- **Service Discovery**: Integration with service registry (optional)
- **Load Balancing**: Client-side load balancing support

### 📊 Observability
- **Request Logging**: Detailed logging of requests and responses
- **Performance Metrics**: Request duration tracking
- **Request ID Propagation**: Unique request ID for distributed tracing
- **Customizable Log Prefixes**: Service-specific log identification

### 🚦 Rate Limiting
- **Token Bucket Algorithm**: Configurable request limits with automatic token refill
- **Distributed Rate Limiting**: Redis-based storage works across multiple gateway instances
- **Multiple Strategies**: Rate limit per user, IP address, API key, or globally
- **Informative Headers**: Returns rate limit status in response headers
- **Graceful Handling**: Returns 429 with retry information when limit exceeded

### ⚡ Performance
- **Reactive & Non-Blocking**: Built on Spring WebFlux for high throughput
- **Async Request Processing**: Handles thousands of concurrent connections
- **Efficient Resource Usage**: Event-loop model minimizes thread overhead

---

## 🗏️ Architecture
```
┌─────────────┐
│   Clients   │
│ (Web/Mobile)│
└──────┬──────┘
       │
       │ HTTP/HTTPS Requests
       │ Authorization: Bearer <JWT>
       ↓
┌──────────────────────────────────────┐
│         API Gateway (Port 8080)      │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  1. Authentication Filter      │  │
│  │     - Validates JWT token      │  │
│  │     - Extracts user info       │  │
│  └────────────────────────────────┘  │
│              ↓                       │
│  ┌────────────────────────────────┐  │
│  │  2. Rate Limiting Filter       │  │
│  │     - Checks request limit     │  │
│  │     - Token bucket algorithm   │  │
│  └────────────────────────────────┘  │
│              ↓                       │
│  ┌────────────────────────────────┐  │
│  │  3. Logging Filter             │  │
│  │     - Logs request/response    │  │
│  │     - Tracks duration          │  │
│  └────────────────────────────────┘  │
│              ↓                       │
│  ┌────────────────────────────────┐  │
│  │  4. Route to Microservice      │  │
│  │     - Forwards with headers    │  │
│  └────────────────────────────────┘  │
└──────────────┬───────────────────────┘
               │
       ┌───────┴────────┐
       │                │
       ↓                ↓
┌──────────────┐  ┌──────────────┐
│ User Service │  │Order Service │
│ (Port 8081)  │  │ (Port 8082)  │
│              │  │              │
│ Receives:    │  │ Receives:    │
│ X-User-Id    │  │ X-User-Id    │
│ X-User-Role  │  │ X-User-Role  │
│ X-User-Email │  │ X-User-Email │
└──────────────┘  └──────────────┘
```

### Request Flow

1. **Client** sends request with JWT token
2. **Authentication Filter** validates token using public key from Authorization Server
3. **Authentication Filter** extracts user claims (ID, role, email) from token
4. **Authentication Filter** adds user info as HTTP headers
5. **Rate Limiting Filter** checks if user/IP is within rate limit
6. **Rate Limiting Filter** consumes token from bucket or returns 429 if exceeded
7. **Logging Filter** logs request details
8. **Gateway** routes to appropriate microservice
9. **Microservice** processes request using user context from headers
10. **Logging Filter** logs response with duration
11. **Gateway** returns response to client

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17+ | Programming Language |
| Spring Boot | 3.2.x | Application Framework |
| Spring Cloud Gateway | 4.1.x | API Gateway |
| Spring WebFlux | 6.x | Reactive Web Framework |
| JJWT | 0.12.5 | JWT Parsing & Validation |
| Nimbus JOSE JWT | 9.37.3 | JWKS Parsing |
| Bucket4j | 8.7.0 | Rate Limiting (Token Bucket) |
| Redis | 7.x | Distributed Rate Limit Storage |
| Project Reactor | 3.6.x | Reactive Programming |
| Lombok | 1.18.x | Boilerplate Reduction |
| Maven | 3.9+ | Build Tool |
---

## 📋 Prerequisites

Before running the API Gateway, ensure you have:

- **Java 17 or higher** installed
- **Maven 3.9+** for building the project
- **Redis** running on port 6379 (for rate limiting)
- **Spring Authorization Server** running on port 9000 (or configured port)
- **Backend Microservices** running (User Service, Order Service, etc.)

### Authorization Server Requirements

The gateway expects a Spring Authorization Server with:
- JWKS endpoint available at: `http://localhost:9000/oauth2/jwks`
- Issuer: `http://localhost:9000`
- RSA-signed JWT tokens (RS256 algorithm)

---


## 📦 Installation

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/api-gateway.git
cd api-gateway
```

### 2. Start Redis
```bash
docker run -d -p 6379:6379 --name redis redis:alpine
```

Or using Docker Compose (see `docker-compose.yml`):
```bash
docker-compose up -d
```

### 3. Build the Project
```bash
mvn clean install
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

## 🚀 Usage

### 1. Get Access Token

First, obtain a JWT token from your Authorization Server:
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" \
  -d "scope=read write"
```

**Response:**
```json
{
  "access_token": "eyJraWQiOiI1ODJmMTExMS02YTU1...",
  "token_type": "Bearer",
  "expires_in": 3599
}
```

### 2. Call Protected Endpoints

Use the access token to call protected endpoints:
```bash
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer eyJraWQiOiI1ODJmMTExMS02YTU1..."
```

### 3. Access Public Endpoints

Public endpoints don't require authentication:
```bash
curl http://localhost:8080/api/public/info
```

---

## 📖 API Documentation

### Authentication

All protected endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <access_token>
```

### Headers Added by Gateway

The gateway adds the following headers to requests forwarded to downstream services:

| Header | Description | Example |
|--------|-------------|---------|
| `X-User-Id` | User identifier from token | `user-123` |
| `X-User-Role` | User role/authorities | `ROLE_USER` |
| `X-User-Email` | User email address | `john@example.com` |
| `X-Authenticated` | Authentication status | `true` |
| `X-Request-Id` | Unique request identifier | `abc-123-def-456` |

### Error Responses

The gateway returns structured JSON error responses:

**401 Unauthorized - Missing Token:**
```json
{
  "error": "Missing authorization header",
  "status": 401,
  "timestamp": "2025-11-04T13:15:00.191199Z"
}
```

**401 Unauthorized - Invalid Format:**
```json
{
  "error": "Invalid authorization format. Expected: Bearer ",
  "status": 401,
  "timestamp": "2025-11-04T13:15:00.191199Z"
}
```

**403 Forbidden - Invalid Token:**
```json
{
  "error": "Invalid or expired token",
  "status": 403,
  "timestamp": "2025-11-04T13:15:00.191199Z"
}
```

**429 Too Many Requests - Rate Limit Exceeded:**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again in 60 seconds.",
  "status": 429,
  "timestamp": "2025-11-04T14:30:00.191199Z"
}
```

### Health Check

Check gateway health:
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### View Routes

List all configured routes:
```bash
curl http://localhost:8080/actuator/gateway/routes
```

---


---

## 🛡️ Rate Limiting

The gateway implements **distributed rate limiting** using the Token Bucket algorithm with Redis.

### How It Works
```
Request → Identify User/IP → Check Token Bucket in Redis → Allow or Deny
```

Each user gets a "bucket" of tokens. Each request consumes 1 token. Tokens automatically refill over time.

### Configuration

**Global settings (application.yml):**
```yaml
rate-limit:
  enabled: true
  default-capacity: 100          # Max tokens in bucket
  default-refill-tokens: 100     # Tokens to add on refill
  default-refill-duration: 60    # Refill every 60 seconds
```

**Per-route settings:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/user/**
          filters:
            - AuthenticationFilter
            - name: RateLimitingFilter
              args:
                keyStrategy: USER      # USER, IP, API_KEY, or GLOBAL
                capacity: 50           # 50 requests max
                refillTokens: 50       # Add 50 tokens
                refillDuration: 60     # Every 60 seconds
            - LoggingFilter
```

### Rate Limit Strategies

| Strategy | Description | Example Key |
|----------|-------------|-------------|
| **USER** | Per authenticated user (from JWT) | `user:123` |
| **IP** | Per client IP address | `ip:192.168.1.1` |
| **API_KEY** | Per API key header | `api-key:abc123` |
| **GLOBAL** | All requests share same bucket | `global` |

### Response Headers

**Normal response:**
```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 50             # Max requests allowed
X-RateLimit-Remaining: 45         # Requests remaining
X-RateLimit-Reset: 1730734920000  # Reset timestamp (Unix ms)
```

**Rate limit exceeded:**
```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 0
Retry-After: 60                   # Seconds to wait

{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again in 60 seconds.",
  "status": 429,
  "timestamp": "2025-11-04T14:30:00.191199Z"
}
```


### Configuration Examples

**Burst and sustained rate:**
```yaml
capacity: 100          # Can burst 100 requests immediately
refillTokens: 100      # Then limited to 100 requests
refillDuration: 60     # Per minute (sustained rate)
```

**Strict rate (no burst):**
```yaml
capacity: 10           # Max 10 requests at once
refillTokens: 10       # Add 10 tokens
refillDuration: 1      # Every second = 10 req/sec
```


## 📁 Project Structure
```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/api_gateway/
│   │   │       ├── ApiGatewayApplication.java      # Main application
│   │   │       ├── config/
│   │   │       │   ├── JwtProperties.java          # JWT configuration
│   │   │       │   ├── RateLimitProperties.java    # Rate limit config
│   │   │       │   └── RedisConfig.java            # Redis connection
│   │   │       ├── service/
│   │   │       │   ├── JwtUtil.java                # JWT validation
│   │   │       │   └── RateLimiterService.java     # Rate limiting logic
│   │   │       ├── filter/
│   │   │       │   ├── RequestIdFilter.java        # Request ID generation
│   │   │       │   ├── AuthenticationFilter.java   # JWT authentication
│   │   │       │   ├── RateLimitingFilter.java     # Rate limiting
│   │   │       │   └── LoggingFilter.java          # Request/response logging
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java # Global error handling
│   │   └── resources/
│   │       ├── application.yml                     # Main configuration
│   │       ├── application-dev.yml                 # Development config
│   │       └── application-prod.yml                # Production config
│   └── test/
│       └── java/
│           └── com/example/api_gateway/
│               └── ApiGatewayApplicationTests.java
├── docker-compose.yml                              # Docker services (Redis)
├── pom.xml                                         # Maven dependencies
├── README.md                                       # This file
└── .gitignore
```


### Key Components

#### `ApiGatewayApplication.java`
Main Spring Boot application entry point.

#### `JwtProperties.java`
Configuration class that maps JWT-related properties from `application.yml` using `@ConfigurationProperties`.

#### `JwtUtil.java`
Utility class for JWT operations:
- Fetches public key from JWKS endpoint at startup
- Validates JWT tokens offline using RSA public key
- Extracts claims (user ID, role, email) from tokens

#### `AuthenticationFilter.java`
Gateway filter that:
- Validates JWT tokens in Authorization header
- Extracts user information from tokens
- Adds user context headers for downstream services
- Returns proper error responses for invalid tokens

#### `LoggingFilter.java`
Route-specific filter that:
- Logs incoming requests with details
- Logs responses with status code and duration
- Supports custom log prefixes per route
- Tracks request performance


#### `RateLimitProperties.java`
Configuration class for rate limiting settings (capacity, refill tokens, refill duration).

#### `RedisConfig.java`
Redis client configuration for distributed rate limiting storage.

#### `RateLimiterService.java`
Core rate limiting service:
- Implements Token Bucket algorithm using Bucket4j
- Manages token buckets in Redis for distributed rate limiting
- Provides token consumption and availability checking

#### `RequestIdFilter.java`
Global filter that generates unique request IDs for distributed tracing.

#### `RateLimitingFilter.java`
Gateway filter that:
- Checks rate limits before forwarding requests
- Supports multiple key strategies (user, IP, API key, global)
- Returns 429 error with retry information when limit exceeded
- Adds rate limit headers to responses
---



