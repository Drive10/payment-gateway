package dev.payment.analyticsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.analyticsservice.entity.*;
import dev.payment.analyticsservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsEventRepository eventRepository;
    private final MetricRepository metricRepository;
    private final ReportRepository reportRepository;
    private final KpiRepository kpiRepository;
    private final RealTimeCounterRepository counterRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
            AnalyticsEventRepository eventRepository,
            MetricRepository metricRepository,
            ReportRepository reportRepository,
            KpiRepository kpiRepository,
            RealTimeCounterRepository counterRepository,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.metricRepository = metricRepository;
        this.reportRepository = reportRepository;
        this.kpiRepository = kpiRepository;
        this.counterRepository = counterRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalyticsEvent recordEvent(String eventType, String category, Map<String, Object> data,
                                      String merchantId, String orderId, String paymentId,
                                      String userId, Double amount, String currency, String status) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setEventType(eventType);
        event.setEventCategory(category);
        event.setMerchantId(merchantId);
        event.setOrderId(orderId);
        event.setPaymentId(paymentId);
        event.setUserId(userId);
        event.setStatus(status);
        event.setCreatedAt(Instant.now());

        try {
            event.setEventData(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            event.setEventData("{}");
        }

        if (amount != null) {
            event.setAmount(java.math.BigDecimal.valueOf(amount));
        }
        event.setCurrency(currency);

        AnalyticsEvent saved = eventRepository.save(event);
        incrementCounter(eventType);
        return saved;
    }

    public Metric recordMetric(String metricName, String metricType, Double value,
                               String merchantId, String currency, String status, String period) {
        Instant now = Instant.now();
        Instant periodStart = getPeriodStart(now, period);
        Instant periodEnd = periodStart.plus(getPeriodDuration(period), ChronoUnit.MINUTES);

        Optional<Metric> existing = metricRepository.findByMetricNameAndDimensionMerchantIdAndPeriodStart(
                metricName, merchantId, periodStart);

        if (existing.isPresent()) {
            Metric metric = existing.get();
            metric.setValue(value);
            metric.setCount(metric.getCount() + 1);
            metric.setSumValue(metric.getSumValue() + value);
            if (metric.getMinValue() == null || value < metric.getMinValue()) {
                metric.setMinValue(value);
            }
            if (metric.getMaxValue() == null || value > metric.getMaxValue()) {
                metric.setMaxValue(value);
            }
            metric.setUpdatedAt(now);
            return metricRepository.save(metric);
        } else {
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
            return metricRepository.save(metric);
        }
    }

    public Report createReport(String reportType, String reportName, Map<String, Object> params,
                              String merchantId, String createdBy) {
        Report report = new Report();
        report.setReportType(reportType);
        report.setReportName(reportName);
        report.setMerchantId(merchantId);
        report.setCreatedBy(createdBy);
        report.setStatus("PENDING");

        try {
            report.setReportParams(objectMapper.writeValueAsString(params));
        } catch (JsonProcessingException e) {
            report.setReportParams("{}");
        }

        return reportRepository.save(report);
    }

    public Report completeReport(Long reportId, Map<String, Object> reportData) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        report.setStatus("COMPLETED");
        report.setCompletedAt(Instant.now());

        try {
            report.setReportData(objectMapper.writeValueAsString(reportData));
        } catch (JsonProcessingException e) {
            report.setReportData("{}");
        }

        return reportRepository.save(report);
    }

    public void updateKpi(String kpiName, Double value, String unit, String merchantId) {
        Optional<Kpi> existing = kpiRepository.findByKpiNameAndDimensionMerchantId(kpiName, merchantId);

        if (existing.isPresent()) {
            Kpi kpi = existing.get();
            kpi.setPreviousValue(kpi.getKpiValue());
            kpi.setKpiValue(value);
            kpi.setKpiUnit(unit);

            if (kpi.getPreviousValue() != null && kpi.getPreviousValue() > 0) {
                double change = ((value - kpi.getPreviousValue()) / kpi.getPreviousValue()) * 100;
                kpi.setChangePercentage(change);
            }

            kpi.setUpdatedAt(Instant.now());
            kpiRepository.save(kpi);
        } else {
            Kpi kpi = new Kpi();
            kpi.setKpiName(kpiName);
            kpi.setKpiValue(value);
            kpi.setKpiUnit(unit);
            kpi.setDimensionMerchantId(merchantId);
            kpiRepository.save(kpi);
        }
    }

    public Map<String, Object> getDashboardSummary(String merchantId) {
        Map<String, Object> summary = new HashMap<>();

        List<Object[]> eventCounts = eventRepository.countByEventType();
        Map<String, Long> eventTypeCounts = new HashMap<>();
        for (Object[] row : eventCounts) {
            eventTypeCounts.put((String) row[0], (Long) row[1]);
        }
        summary.put("eventCounts", eventTypeCounts);

        List<Object[]> categoryCounts = eventRepository.countByEventCategory();
        Map<String, Long> categoryTypeCounts = new HashMap<>();
        for (Object[] row : categoryCounts) {
            categoryTypeCounts.put((String) row[0], (Long) row[1]);
        }
        summary.put("categoryCounts", categoryTypeCounts);

        if (merchantId != null) {
            List<Metric> recentMetrics = metricRepository.findByDimensionMerchantId(merchantId);
            summary.put("recentMetrics", recentMetrics);

            List<Kpi> kpis = kpiRepository.findByDimensionMerchantId(merchantId);
            summary.put("kpis", kpis);
        }

        return summary;
    }

    public List<AnalyticsEvent> getEvents(String merchantId, String eventType,
                                          Instant start, Instant end, int limit) {
        List<AnalyticsEvent> events;
        if (merchantId != null) {
            events = eventRepository.findByMerchantId(merchantId);
        } else if (eventType != null) {
            events = eventRepository.findByEventType(eventType);
        } else if (start != null && end != null) {
            events = eventRepository.findByDateRange(start, end);
        } else {
            events = eventRepository.findAll();
        }
        return events.size() > limit ? events.subList(0, limit) : events;
    }

    public Map<String, Long> getRealTimeCounters() {
        List<RealTimeCounter> counters = counterRepository.findAll();
        Map<String, Long> result = new HashMap<>();
        for (RealTimeCounter counter : counters) {
            result.put(counter.getCounterName(), counter.getCounterValue());
        }
        return result;
    }

    public void incrementCounter(String name) {
        Optional<RealTimeCounter> existing = counterRepository.findByCounterName(name);
        if (existing.isPresent()) {
            counterRepository.incrementCounter(name);
        } else {
            RealTimeCounter counter = new RealTimeCounter();
            counter.setCounterName(name);
            counter.setCounterValue(1L);
            counterRepository.save(counter);
        }
    }

    public Optional<Report> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    public List<Report> getReports(String merchantId, String status) {
        List<Report> reports;

        if (merchantId != null && status != null) {
            reports = reportRepository.findByMerchantIdAndStatus(merchantId, status);
        } else if (merchantId != null) {
            reports = reportRepository.findByMerchantId(merchantId);
        } else if (status != null) {
            reports = reportRepository.findByStatus(status);
        } else {
            reports = reportRepository.findAll();
        }

        return reports;
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
