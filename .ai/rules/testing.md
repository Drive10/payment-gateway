# Testing Rules

> Test standards and patterns for PayFlow

---

## 1. Coverage Targets

| Type | Minimum | Target |
|------|---------|--------|
| Unit Tests | 70% | 80% |
| Integration Tests | Required | Full coverage |
| E2E Tests | Critical flows | All user paths |

---

## 2. Test Organization

### Directory Structure

```
src/test/java/com/payflow/{service}/
├── unit/
│   ├── service/
│   ├── controller/
│   └── util/
├── integration/
│   ├── controller/
│   └── repository/
└── TestData.java
```

---

## 3. Unit Testing Patterns

### Service Test

```java
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository repository;
    
    @Mock
    private PaymentProvider provider;
    
    @InjectMocks
    private PaymentService service;
    
    @Test
    void processPayment_ValidRequest_ReturnsSuccess() {
        // Arrange
        PaymentRequest request = PaymentRequest.builder()
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .build();
            
        when(provider.process(any()))
            .thenReturn(PaymentResult.success("txn_123"));
            
        // Act
        PaymentResult result = service.process(request);
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals("txn_123", result.getTransactionId());
    }
}
```

### Controller Test

```java
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PaymentService service;
    
    @Test
    void createPayment_ValidRequest_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100, \"currency\": \"USD\"}"))
            .andExpect(status().isCreated());
    }
}
```

---

## 4. Test Naming

```
MethodName_StateUnderTest_ExpectedBehavior

Examples:
processPayment_ValidCard_ReturnsSuccess
processPayment_ExpiredCard_ThrowsException
processPayment_ProviderTimeout_RetriesThreeTimes
```

---

## 5. Integration Testing

### Testcontainers

```java
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Container
    static RedisContainer redis = 
        new RedisContainer("redis:7-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.data.redis.url", redis::getRedisUrl);
    }
}
```

---

## 6. Test Fixtures

### Builder Pattern

```java
public class PaymentRequestBuilder {
    private BigDecimal amount = new BigDecimal("100.00");
    private String currency = "USD";
    private String customerEmail = "test@example.com";
    
    public PaymentRequestBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }
    
    public PaymentRequest build() {
        return new PaymentRequest(amount, currency, customerEmail);
    }
}
```

---

## 7. What to Test

### Must Test

- ✅ Happy path
- ✅ Validation errors
- ✅ Provider failures
- ✅ Timeout scenarios
- ✅ Null/empty inputs
- ✅ Edge cases

### Don't Test

- ❌ Third-party library internals
- ❌ Simple getters/setters
- ❌ Configuration loading

---

## 8. Frontend Testing

### Component Tests

```tsx
test('renders payment form', () => {
  render(<PaymentForm {...defaultProps} />);
  expect(screen.getByLabelText('Card Number')).toBeInTheDocument();
});

test('submits on button click', async () => {
  const onSuccess = jest.fn();
  render(<PaymentForm {...defaultProps} onSuccess={onSuccess} />);
  
  await userEvent.click(screen.getByRole('button'));
  await waitFor(() => expect(onSuccess).toHaveBeenCalled());
});
```

---

## Quick Reference

| Type | Framework | Target |
|------|-----------|--------|
| Unit | JUnit 5 + Mockito | 80% |
| Integration | Testcontainers | Full |
| E2E | Playwright | Critical paths |
| Frontend | Jest + Testing Library | 80% |