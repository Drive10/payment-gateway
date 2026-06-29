package dev.payment.paymentservice.service;

import dev.payment.paymentservice.service.FraudDetectionService.FraudCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FraudDetectionServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    private FraudDetectionService fraudService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        fraudService = new FraudDetectionService(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void check_NormalRequest_ShouldReturnAllowed() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        FraudCheckResult result = fraudService.check("test@example.com", "192.168.1.1",
                new BigDecimal("1000"), "1234");

        assertTrue(result.allowed());
        assertFalse(result.review());
        assertNull(result.reason());
    }

    @Test
    void check_ExceededMinuteRate_ShouldReturnDenied() {
        when(valueOperations.increment(anyString())).thenReturn(11L);

        FraudCheckResult result = fraudService.check("test@example.com", "192.168.1.1",
                new BigDecimal("1000"), "1234");

        assertFalse(result.allowed());
        assertEquals("Rate limit exceeded: too many transactions per minute", result.reason());
    }

    @Test
    void check_ExceededHourRate_ShouldReturnDenied() {
        when(valueOperations.increment(contains("txn_minute"))).thenReturn(1L);
        when(valueOperations.increment(contains("txn_hour"))).thenReturn(51L);
        when(valueOperations.increment(contains("ip"))).thenReturn(1L);

        FraudCheckResult result = fraudService.check("test@example.com", "192.168.1.1",
                new BigDecimal("1000"), "1234");

        assertFalse(result.allowed());
        assertEquals("Rate limit exceeded: too many transactions per hour", result.reason());
    }

    @Test
    void check_ExceededIpRate_ShouldReturnDenied() {
        when(valueOperations.increment(contains("txn_minute"))).thenReturn(1L);
        when(valueOperations.increment(contains("txn_hour"))).thenReturn(1L);
        when(valueOperations.increment(contains("ip"))).thenReturn(21L);

        FraudCheckResult result = fraudService.check("test@example.com", "192.168.1.1",
                new BigDecimal("1000"), "1234");

        assertFalse(result.allowed());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("IP"));
    }

    @Test
    void check_HighAmount_ShouldReturnReview() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        FraudCheckResult result = fraudService.check("test@example.com", "192.168.1.1",
                new BigDecimal("200000"), "1234");

        assertFalse(result.allowed());
        assertTrue(result.review());
        assertEquals("Amount exceeds threshold, requires manual review", result.reason());
    }

    @Test
    void check_AmountAtThreshold_ShouldReturnAllowed() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        FraudCheckResult result = fraudService.check("test@example.com", "192.168.1.1",
                new BigDecimal("100000"), "1234");

        assertTrue(result.allowed());
    }

    @Test
    void recordFailedAttempt_ShouldIncrementCounter() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        fraudService.recordFailedAttempt("test@example.com");

        verify(valueOperations, times(1)).increment(contains("fail_hour"));
        verify(redisTemplate, times(1)).expire(anyString(), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void check_ShouldSetExpiryOnRedisKeys() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        fraudService.check("test@example.com", "192.168.1.1", new BigDecimal("1000"), "1234");

        verify(redisTemplate, times(1)).expire(contains("txn_minute"), eq(1L), eq(TimeUnit.MINUTES));
        verify(redisTemplate, times(1)).expire(contains("txn_hour"), eq(1L), eq(TimeUnit.HOURS));
        verify(redisTemplate, times(1)).expire(contains("ip"), eq(1L), eq(TimeUnit.MINUTES));
    }
}
