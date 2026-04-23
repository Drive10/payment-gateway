package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.dto.response.MerchantAnalyticsResponse;
import dev.payment.paymentservice.payment.dto.response.PaymentTrendsResponse;
import dev.payment.paymentservice.payment.repository.PaymentRefundRepository;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAnalyticsService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;

    public PaymentAnalyticsService(PaymentRepository paymentRepository, PaymentRefundRepository paymentRefundRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
    }

    public MerchantAnalyticsResponse getMerchantAnalytics(UUID merchantId) {
        BigDecimal totalRevenue = paymentRepository.sumCapturedAmountByMerchantId(merchantId);
        BigDecimal totalFees = paymentRepository.sumPlatformFeeByMerchantId(merchantId);
        BigDecimal totalRefunds = paymentRefundRepository.sumRefundedAmountByMerchantId(merchantId);

        long totalPayments = paymentRepository.countByMerchantId(merchantId);
        long successCount = paymentRepository.countByMerchantIdAndStatus(merchantId, PaymentStatus.CAPTURED);
        long failedCount = paymentRepository.countByMerchantIdAndStatus(merchantId, PaymentStatus.FAILED);

        BigDecimal netRevenue = calculateNetRevenue(totalRevenue, totalFees, totalRefunds);
        double successRate = calculateSuccessRate(totalPayments, successCount);

        log.info("analytics=merchant merchantId={} totalPayments={} successRate={}",
                merchantId, totalPayments, successRate);

        return new MerchantAnalyticsResponse(
                orZero(totalRevenue),
                orZero(totalFees),
                orZero(totalRefunds),
                netRevenue,
                totalPayments,
                successCount,
                failedCount,
                successRate
        );
    }

    public PaymentTrendsResponse getPaymentTrends(UUID merchantId, int days) {
        List<PaymentTrendsResponse.DailyTrend> trends = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).with(LocalTime.MIN);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            BigDecimal dayRevenue = paymentRepository.sumCapturedAmountByMerchantIdAndDateRange(merchantId, dayStart, dayEnd);
            long dayCount = paymentRepository.countByMerchantIdAndDateRange(merchantId, dayStart, dayEnd);

            trends.add(new PaymentTrendsResponse.DailyTrend(
                    dayStart.toLocalDate(),
                    orZero(dayRevenue),
                    dayCount
            ));
        }

        log.info("analytics=trends merchantId={} days={} trendCount={}", merchantId, days, trends.size());
        return new PaymentTrendsResponse(trends);
    }

    private BigDecimal calculateNetRevenue(BigDecimal revenue, BigDecimal fees, BigDecimal refunds) {
        BigDecimal totalRevenue = orZero(revenue);
        BigDecimal totalFees = orZero(fees);
        BigDecimal totalRefunds = orZero(refunds);
        return totalRevenue.subtract(totalFees).subtract(totalRefunds);
    }

    private double calculateSuccessRate(long total, long success) {
        if (total <= 0) return 0.0;
        return Math.round((double) success / total * 10000.0) / 100.0;
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
