# Backend Rules

> Java/Spring development standards for PayFlow

---

## 1. Java Standards

### General
- Use **Lombok** for boilerplate reduction
- Follow **Google Java Style Guide**
- 4-space indentation, max line length: 100
- Use **Spring Data JPA** for persistence
- Use **Resilience4j** for circuit breaker and retry

### Package Structure

```
com.payflow.{service}/
├── controller/     # REST controllers
├── service/       # Business logic
├── repository/    # Data access
├── model/         # JPA entities
├── dto/           # Data transfer objects
├── config/        # Configuration
├── exception/     # Custom exceptions
└── util/          # Utilities
```

---

## 2. Service Layer

### Pattern

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentProvider provider;
    private final EventPublisher events;
    
    @Transactional
    public PaymentResult process(PaymentRequest request) {
        // 1. Validate request
        // 2. Execute payment
        // 3. Save result
        // 4. Publish event
        // 5. Return result
    }
}
```

---

## 3. Controller Layer

### REST Controllers

```java
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    
    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentRequest request,
            @AuthPrincipal User user) {
        PaymentResult result = service.process(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PaymentResponse.from(result));
    }
}
```

---

## 4. Resilience Patterns

### Circuit Breaker

```java
@CircuitBreaker(name = "paymentProvider", fallbackMethod = "fallback")
public PaymentResult callProvider(PaymentRequest request) {
    return provider.process(request);
}

private PaymentResult fallback(PaymentRequest request, Exception ex) {
    log.error("Payment provider unavailable: {}", ex.getMessage());
    return PaymentResult.failure("SERVICE_UNAVAILABLE");
}
```

### Retry with Backoff

```java
@Retry(name = "paymentRetry", maxAttempts = 3, waitDuration = @Duration("2s"))
public PaymentResult processWithRetry(PaymentRequest request) {
    return provider.process(request);
}
```

---

## 5. Event Publishing

### Kafka Events

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    public void completePayment(Payment payment) {
        // ... process payment
        
        PaymentEvent event = PaymentEvent.builder()
            .paymentId(payment.getId())
            .status(payment.getStatus())
            .timestamp(Instant.now())
            .build();
            
        kafkaTemplate.send("payment.completed", payment.getId(), event);
    }
}
```

---

## 6. Error Handling

### Custom Exceptions

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentException extends RuntimeException {
    private final ErrorCode code;
    
    public PaymentException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }
}
```

---

## 7. Configuration

### Application.yml

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:payflow}
    username: ${DB_USERNAME:payflow}
    password: ${DB_PASSWORD:payflow}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true

resilience4j:
  circuitbreaker:
    instances:
      paymentProvider:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

---

## Quick Reference

| Pattern | Implementation |
|---------|---------------|
| Service | @Service + @Transactional |
| Controller | @RestController + @RequestMapping |
| Repository | @Repository + extends JpaRepository |
| Circuit Breaker | @CircuitBreaker + fallback |
| Retry | @Retry + exponential backoff |
| Events | KafkaTemplate + @KafkaListener |