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

### ⚡ Performance
- **Reactive & Non-Blocking**: Built on Spring WebFlux for high throughput
- **Async Request Processing**: Handles thousands of concurrent connections
- **Efficient Resource Usage**: Event-loop model minimizes thread overhead

---

## 🏗️ Architecture
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
│  ┌────────────────────────────────┐ │
│  │  1. Authentication Filter      │ │
│  │     - Validates JWT token      │ │
│  │     - Extracts user info       │ │
│  └────────────────────────────────┘ │
│              ↓                       │
│  ┌────────────────────────────────┐ │
│  │  2. Logging Filter             │ │
│  │     - Logs request/response    │ │
│  │     - Tracks duration          │ │
│  └────────────────────────────────┘ │
│              ↓                       │
│  ┌────────────────────────────────┐ │
│  │  3. Route to Microservice      │ │
│  │     - Forwards with headers    │ │
│  └────────────────────────────────┘ │
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
5. **Logging Filter** logs request details
6. **Gateway** routes to appropriate microservice
7. **Microservice** processes request using user context from headers
8. **Logging Filter** logs response with duration
9. **Gateway** returns response to client

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
| Project Reactor | 3.6.x | Reactive Programming |
| Lombok | 1.18.x | Boilerplate Reduction |
| Maven | 3.9+ | Build Tool |

---

## 📋 Prerequisites

Before running the API Gateway, ensure you have:

- **Java 17 or higher** installed
- **Maven 3.9+** for building the project
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

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run the Application
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


## 📁 Project Structure
```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/api_gateway/
│   │   │       ├── ApiGatewayApplication.java      # Main application
│   │   │       ├── config/
│   │   │       │   └── JwtProperties.java          # JWT configuration properties
│   │   │       ├── util/
│   │   │       │   └── JwtUtil.java                # JWT validation utility
│   │   │       ├── filter/
│   │   │       │   ├── AuthenticationFilter.java   # JWT authentication filter
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

---

