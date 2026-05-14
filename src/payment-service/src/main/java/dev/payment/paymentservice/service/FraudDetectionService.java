package dev.payment.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_TXNS_PER_MINUTE = 10;
    private static final int MAX_TXNS_PER_HOUR = 50;
    private static final BigDecimal MAX_AMOUNT_PER_TXN = new BigDecimal("100000");
    private static final int MAX_FAILED_TXNS_PER_HOUR = 5;

    public FraudCheckResult check(String customerEmail, String ipAddress, BigDecimal amount, String cardLast4) {
        String minuteKey = "fraud:txn_minute:" + customerEmail + ":" + (System.currentTimeMillis() / 60000);
        String hourKey = "fraud:txn_hour:" + customerEmail + ":" + (System.currentTimeMillis() / 3600000);
        String failHourKey = "fraud:fail_hour:" + customerEmail + ":" + (System.currentTimeMillis() / 3600000);
        String ipKey = "fraud:ip:" + ipAddress + ":" + (System.currentTimeMillis() / 60000);

        long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
        redisTemplate.expire(minuteKey, 1, TimeUnit.MINUTES);

        long hourCount = redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, 1, TimeUnit.HOURS);

        long ipCount = redisTemplate.opsForValue().increment(ipKey);
        redisTemplate.expire(ipKey, 1, TimeUnit.MINUTES);

        if (minuteCount > MAX_TXNS_PER_MINUTE) {
            log.warn("Fraud: rate limit per minute exceeded for {}", customerEmail);
            return FraudCheckResult.DENIED("Rate limit exceeded: too many transactions per minute");
        }

        if (hourCount > MAX_TXNS_PER_HOUR) {
            log.warn("Fraud: rate limit per hour exceeded for {}", customerEmail);
            return FraudCheckResult.DENIED("Rate limit exceeded: too many transactions per hour");
        }

        if (ipCount > 20) {
            log.warn("Fraud: IP rate limit exceeded for {}", ipAddress);
            return FraudCheckResult.DENIED("Rate limit exceeded from this IP address");
        }

        if (amount.compareTo(MAX_AMOUNT_PER_TXN) > 0) {
            log.warn("Fraud: amount threshold exceeded {} for {}", amount, customerEmail);
            return FraudCheckResult.REVIEW("Amount exceeds threshold, requires manual review");
        }

        return FraudCheckResult.ALLOWED;
    }

    public void recordFailedAttempt(String customerEmail) {
        String failHourKey = "fraud:fail_hour:" + customerEmail + ":" + (System.currentTimeMillis() / 3600000);
        long failCount = redisTemplate.opsForValue().increment(failHourKey);
        redisTemplate.expire(failHourKey, 1, TimeUnit.HOURS);

        if (failCount > MAX_FAILED_TXNS_PER_HOUR) {
            log.warn("Fraud: too many failed attempts for {}", customerEmail);
        }
    }

    public record FraudCheckResult(boolean allowed, boolean review, String reason) {
        public static final FraudCheckResult ALLOWED = new FraudCheckResult(true, false, null);

        public static FraudCheckResult DENIED(String reason) {
            return new FraudCheckResult(false, false, reason);
        }

        public static FraudCheckResult REVIEW(String reason) {
            return new FraudCheckResult(false, true, reason);
        }
    }
}
