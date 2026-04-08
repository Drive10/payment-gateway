# PayFlow Services Reference

> Complete reference for all microservices in PayFlow payment gateway

---

## Services Overview

| Service | Port | Tech | Description |
|---------|------|------|-------------|
| **api-gateway** | 8080 | Spring Cloud Gateway | Central routing, JWT auth, rate limiting |
| **auth-service** | 8081 | Spring Boot 3 | User auth, JWT, RBAC, sessions |
| **order-service** | 8082 | Spring Boot 3 | Order management, merchants |
| **payment-service** | 8083 | Spring Boot 3 | Payment orchestration, multi-provider |
| **notification-service** | 8084 | Spring Boot 3 | Email, SMS, push, webhooks |
| **simulator-service** | 8086 | Spring Boot 3 | Payment simulation, testing |
| **analytics-service** | 8089 | Spring Boot 3 | Risk, settlements, reports |
| **audit-service** | 8090 | Spring Boot 3 | MongoDB audit logging |

---

## API Gateway (8080)

### Filters
- `JwtAuthenticationFilter` - JWT token validation
- `InternalGatewayAuthFilter` - Internal service auth
- `TenantRateLimitingFilter` - Per-tenant rate limiting
- `UserRateLimitFilter` - Per-user rate limiting
- `SecurityHeadersFilter` - Security headers
- `RequestValidationFilter` - Request validation
- `CorrelationIdFilter` - Request correlation IDs

### Configuration
- `CorsConfig` - CORS policy
- `GatewayJwtService` - JWT service for gateway

### Endpoints
- `/api/v1/*` → Routes to services
- `/actuator/health` → Health check

---

## Auth Service (8081)

### Controllers
- `AuthController` - Authentication endpoints
- `PlatformUserController` - User management

### Services
- `AuthService` - Core authentication logic
- `UserService` - User management
- `JwtService` - JWT token generation/validation

### Security
- `SecurityConfig` - Spring Security configuration
- `JwtConfig` - JWT configuration
- `InternalApiAuthFilter` - Internal API authentication

### Entities
- `User` - User entity
- `Role` - Role entity
- `UserPrincipal` - Principal for authentication

### Repositories
- `UserRepository` - User data access
- `RoleRepository` - Role data access

### DTOs
- `RegisterRequest` - Registration request
- `LoginRequest` - Login request
- `AuthResponse` - Auth response with tokens
- `UserResponse` - User data response

---

## Payment Service (8083)

### Core Services
- `PaymentService` - Payment orchestration
- `PaymentStateMachine` - Payment state transitions
- `SagaOrchestrator` - Saga pattern for distributed transactions

### Integration
- `WebhookService` - Webhook handling
- `RazorpayWebhookService` - Razorpay-specific webhooks
- `PaymentStatusListener` - Payment status updates

### Reconciliation
- `PaymentReconciliationJob` - Scheduled reconciliation
- `PaymentReconciliationClient` - Client for reconciliation
- `PaymentReconciliationProvider` - Provider interface
- `RazorpayPaymentReconciliationProvider` - Razorpay implementation
- `SimulatorPaymentReconciliationProvider` - Simulator implementation
- `ProviderPaymentSnapshot` - Provider state snapshots

### Outbox
- `PaymentOutboxService` - Outbox pattern for reliable events

---

## Order Service (8082)

### Functionality
- Order creation and management
- Merchant management
- API key generation
- KYC (Know Your Customer) verification

---

## Notification Service (8084)

### Functionality
- Email notifications
- SMS notifications
- Push notifications
- Webhook delivery
- Feature flags (Redis-based)

---

## Simulator Service (8086)

### Models
- `SimulationTransaction` - Simulation transaction
- `SimulationStatus` - Simulation status enum
- `SimulationMode` - Simulation mode enum

### DTOs
- `CreateSimulationRequest` - Create simulation request
- `SimulationResponse` - Simulation response
- `WebhookCallbackRequest` - Webhook callback

### Controllers
- `SimulatorController` - Simulation endpoints

---

## Analytics Service (8089)

### Entities
- `Settlement` - Merchant settlements
- `SettlementTransaction` - Settlement transactions
- `RiskRule` - Risk scoring rules
- `RiskAssessment` - Risk assessments
- `Dispute` - Payment disputes
- `MerchantSettlement` - Merchant-specific settlements
- `Report` - Analytics reports
- `RealTimeCounter` - Real-time counters
- `Metric` - Analytics metrics
- `Kpi` - Key performance indicators
- `AnalyticsEvent` - Analytics events

### Services
- `AnalyticsService` - Core analytics
- `SettlementService` - Settlement processing
- `SettlementScheduler` - Scheduled settlements
- `RiskScoringService` - Risk scoring
- `DisputeService` - Dispute management

### Controllers
- `AnalyticsController` - Analytics endpoints
- `SettlementController` - Settlement endpoints
- `RiskController` - Risk endpoints
- `DisputeController` - Dispute endpoints

### Repositories
- `SettlementRepository`
- `RiskRuleRepository`
- `RiskAssessmentRepository`
- `DisputeRepository`
- `MerchantSettlementRepository`
- `ReportRepository`
- `RealTimeCounterRepository`
- `MetricRepository`
- `KpiRepository`
- `AnalyticsEventRepository`

---

## Audit Service (8090)

### Document
- `AuditLog` - MongoDB audit log document

### Services
- `AuditService` - Audit logging
- `KafkaAuditConsumer` - Kafka consumer for audit events

### Controllers
- `AuditController` - Audit log endpoints

### Configuration
- `KafkaConfig` - Kafka configuration

---

## Communication Patterns

### Synchronous (Feign Client)
- Auth → Order
- Order → Payment
- API Gateway → All services

### Asynchronous (Kafka)
- Payment → Notification
- Payment → Analytics
- Payment → Search
- Payment → Audit

---

## Kafka Topics

| Topic | Description |
|-------|-------------|
| `payment.created` | New payment created |
| `payment.completed` | Payment succeeded |
| `payment.failed` | Payment failed |
| `payment.refunded` | Payment refunded |
| `order.created` | New order created |
| `order.updated` | Order status changed |
| `webhook.updates` | Webhook delivery events |
| `audit.events` | Audit log events |

---

## Port Mapping

| Service | HTTP Port | Database |
|---------|-----------|----------|
| api-gateway | 8080 | Redis |
| auth-service | 8081 | PostgreSQL, Redis |
| order-service | 8082 | PostgreSQL, Redis |
| payment-service | 8083 | PostgreSQL, Redis |
| notification-service | 8084 | PostgreSQL, Redis |
| simulator-service | 8086 | None |
| analytics-service | 8089 | PostgreSQL, Redis |
| audit-service | 8090 | MongoDB |