package dev.payment.analyticsservice.service;

import dev.payment.analyticsservice.entity.Metric;
import dev.payment.analyticsservice.repository.MetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MetricService {

    private final MetricRepository metricRepository;

    public MetricService(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public List<Map<String, Object>> getRecentMetrics(String merchantId) {
        return metricRepository.findByDimensionMerchantId(merchantId).stream()
                .map(this::toMap)
                .toList();
    }

    private Map<String, Object> toMap(Metric metric) {
        return Map.of(
                "name", metric.getMetricName(),
                "type", metric.getMetricType(),
                "value", metric.getValue(),
                "period", metric.getDimensionPeriod()
        );
    }

    @Transactional
    public Metric recordMetric(String metricName, String metricType, Double value,
                               String merchantId, String currency, String status, String period) {
        Instant now = Instant.now();
        Instant periodStart = getPeriodStart(now, period);
        Instant periodEnd = periodStart.plus(getPeriodDuration(period), ChronoUnit.MINUTES);

        return metricRepository
                .findByMetricNameAndDimensionMerchantIdAndPeriodStart(metricName, merchantId, periodStart)
                .map(metric -> updateMetric(metric, value, now))
                .orElseGet(() -> createMetric(metricName, metricType, value, merchantId, currency, status, period, periodStart, periodEnd));
    }

    private Metric updateMetric(Metric metric, Double value, Instant now) {
        metric.setValue(value);
        metric.setCount(metric.getCount() + 1);
        metric.setSumValue(metric.getSumValue() + value);
        
        metric.setMinValue(Optional.ofNullable(metric.getMinValue())
                .map(min -> Math.min(min, value))
                .orElse(value));
        metric.setMaxValue(Optional.ofNullable(metric.getMaxValue())
                .map(max -> Math.max(max, value))
                .orElse(value));
        
        metric.setUpdatedAt(now);
        return metricRepository.save(metric);
    }

    private Metric createMetric(String metricName, String metricType, Double value,
                               String merchantId, String currency, String status, String period,
                               Instant periodStart, Instant periodEnd) {
        Metric metric = new Metric();
        metric.setMetricName(metricName);
        metric.setMetricType(metricType);
        metric.setValue(value);
        metric.setDimensionMerchantId(merchantId);
        metric.setDimensionCurrency(currency);
        metric.setDimensionStatus(status);
        metric.setDimensionPeriod(period);
        metric.setPeriodStart(periodStart);
        metric.setPeriodEnd(periodEnd);
        metric.setMinValue(value);
        metric.setMaxValue(value);
        metric.setSumValue(value);
        metric.setCount(1L);
        return metricRepository.save(metric);
    }

    private Instant getPeriodStart(Instant now, String period) {
        return switch (period.toLowerCase()) {
            case "minute" -> now.truncatedTo(ChronoUnit.MINUTES);
            case "hour" -> now.truncatedTo(ChronoUnit.HOURS);
            case "day" -> now.truncatedTo(ChronoUnit.DAYS);
            default -> now.truncatedTo(ChronoUnit.HOURS);
        };
    }

    private long getPeriodDuration(String period) {
        return switch (period.toLowerCase()) {
            case "minute" -> 1;
            case "hour" -> 60;
            case "day" -> 1440;
            default -> 60;
        };
    }
}
