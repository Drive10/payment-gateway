# Testing Guide

This document describes the comprehensive testing strategy for the Payment Gateway project.

## Test Layers

| Layer | Framework | Description |
|-------|-----------|-------------|
| **Unit Tests** | JUnit 5 + Mockito | Test individual services in isolation |
| **Integration Tests** | Testcontainers | Test with real databases (PostgreSQL, Kafka) |
| **API Tests** | Rest Assured | HTTP-level tests for API endpoints |
| **Contract Tests** | Pact | Consumer-driven contract testing |
| **E2E Tests** | Playwright | Browser-based UI tests |
| **Load Tests** | k6 | Performance and load testing |

## Running Tests

### All Tests
```bash
# Using the unified test runner
./scripts/run-tests.sh --all

# Using Makefile
make test-all
```

### Individual Test Suites

```bash
# Unit tests (JUnit + Mockito)
./scripts/run-tests.sh --unit
# or
make test-unit

# Integration tests (Testcontainers)
./scripts/run-tests.sh --integration
# or
make test-integration

# API tests (Rest Assured)
./scripts/run-tests.sh --api
# or
make test-api API_BASE_URL=http://localhost:8080

# Contract tests (Pact)
./scripts/run-tests.sh --contract
# or
make test-contract

# E2E tests (Playwright)
./scripts/run-tests.sh --e2e
# or
make test-e2e

# Load tests (k6)
./scripts/run-tests.sh --load
# or
make test-load LOAD_VUS=100 LOAD_DURATION=60
```

### Service-Specific Unit Tests

```bash
# Auth service
cd services/auth-service && mvn test

# Payment service
cd services/payment-service && mvn test

# Risk service
cd services/risk-service && mvn test

# Settlement service
cd services/settlement-service && mvn test

# All service unit tests
make test-services
```

## Test Directory Structure

```
tests/
├── api/                    # Rest Assured API tests
│   ├── src/test/java/
│   │   └── dev/payment/api/
│   │       ├── auth/       # Auth API tests
│   │       ├── orders/     # Orders API tests
│   │       ├── payments/   # Payments API tests
│   │       └── health/     # Health check tests
│   └── pom.xml
│
├── integration/           # Testcontainers integration tests
│   ├── src/test/java/
│   │   └── dev/payment/integration/
│   └── pom.xml
│
├── contracts/              # Pact contract tests
│   ├── src/test/
│   │   ├── java/
│   │   │   └── dev/payment/contracts/
│   │   └── resources/contracts/
│   └── pom.xml
│
├── e2e/                    # Playwright UI tests
│   ├── specs/              # Test specifications
│   ├── pages/              # Page objects
│   ├── fixtures/           # Test fixtures
│   ├── playwright.config.ts
│   └── package.json
│
├── load/                   # k6 load tests
│   ├── scenarios/          # Load test scenarios
│   └── load-test.sh        # Load test runner
│
└── scripts/
    └── run-tests.sh         # Unified test runner
```

## Test Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `API_BASE_URL` | `http://localhost:8080` | Base URL for API tests |
| `LOAD_VUS` | `50` | Number of virtual users for load tests |
| `LOAD_DURATION` | `60` | Duration of load tests in seconds |

### Application Profiles for Tests

- `application-test.yml` - Test configuration for each service
- Uses H2 in-memory database for fast tests
- Uses Testcontainers for integration tests

## Writing Tests

### Unit Test Example (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_shouldCreateWithCorrectStatus() {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(BigDecimal.valueOf(1000));

        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        Payment result = paymentService.createPayment(request);

        // Then
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }
}
```

### API Test Example (Rest Assured)

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApiTest extends ApiTestBase {

    @Test
    @Order(1)
    void registerUser_shouldReturn201() {
        given().spec(spec)
            .contentType("application/json")
            .body("""
                {
                    "email": "test@example.com",
                    "password": "TestPass123!",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """)
            .when()
            .post("/auth/register")
            .then()
            .statusCode(200)
            .body("accessToken", notNullValue());
    }
}
```

### E2E Test Example (Playwright)

```typescript
test('admin can view dashboard', async ({ page, adminDashboard }) => {
  await adminDashboard.goto();
  await adminDashboard.expectLoaded();
  
  const revenue = await adminDashboard.getRevenueValue();
  expect(revenue).toBeGreaterThan(0);
});
```

### Load Test Example (k6)

```javascript
export default function () {
  const payload = JSON.stringify({
    amount: Math.random() * 10000,
    currency: 'USD'
  });

  const res = http.post(
    `${BASE_URL}/api/v1/payments`,
    payload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'payment created': (r) => r.status === 200
  });
}
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run unit tests
        run: make test-unit
        
      - name: Run API tests
        run: make test-api
        env:
          API_BASE_URL: http://localhost:8080
          
      - name: Run E2E tests
        run: make test-e2e
        
      - name: Run load tests
        run: make test-load
        env:
          LOAD_VUS: 100
          LOAD_DURATION: 60
```

## Coverage Reports

Generate coverage reports with:

```bash
# Unit test coverage
make coverage-unit

# Integration test coverage  
make coverage-integration
```

Reports are generated in `target/site/jacoco/index.html` for each service.

## Best Practices

1. **Unit tests first** - Test business logic in isolation
2. **Integration tests for data access** - Test repository queries with Testcontainers
3. **API tests for contracts** - Ensure API endpoints work correctly
4. **E2E tests for critical flows** - Test complete user journeys
5. **Load tests in staging** - Run performance tests in non-production
6. **Mock external services** - Use Pact for contract testing between services
7. **Keep tests fast** - Unit tests should run in seconds
8. **Use test data factories** - Create reusable test data builders
