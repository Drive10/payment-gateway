package dev.payment.analyticsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.analyticsservice.entity.AnalyticsEvent;
import dev.payment.analyticsservice.entity.Metric;
import dev.payment.analyticsservice.entity.Report;
import dev.payment.analyticsservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final AnalyticsEventRepository eventRepository;
    private final ReportRepository reportRepository;
    private final RealTimeCounterRepository counterRepository;
    private final MetricService metricService;
    private final KpiService kpiService;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
            AnalyticsEventRepository eventRepository,
            ReportRepository reportRepository,
            RealTimeCounterRepository counterRepository,
            MetricService metricService,
            KpiService kpiService,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.reportRepository = reportRepository;
        this.counterRepository = counterRepository;
        this.metricService = metricService;
        this.kpiService = kpiService;
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
        event.setAmount(amount != null ? java.math.BigDecimal.valueOf(amount) : null);
        event.setCurrency(currency);
        event.setStatus(status);
        event.setProcessedAt(Instant.now());

        try {
            event.setEventData(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            event.setEventData("{}");
        }

        log.info("analytics=event_recorded type={} merchantId={} paymentId={}", eventType, merchantId, paymentId);
        return eventRepository.save(event);
    }

    public MetricService getMetricService() {
        return metricService;
    }

    public KpiService getKpiService() {
        return kpiService;
    }

    public Map<String, Object> getDashboardSummary(String merchantId) {
        Map<String, Object> summary = new HashMap<>();

        eventRepository.countByEventType().forEach(row ->
                Optional.ofNullable(summary.get("eventCounts"))
                        .filter(m -> m instanceof Map)
                        .map(m -> (Map<String, Long>) m)
                        .ifPresentOrElse(
                                m -> m.put((String) row[0], (Long) row[1]),
                                () -> summary.put("eventCounts", new HashMap<>(Map.of((String) row[0], (Long) row[1])))
                        )
        );

        eventRepository.countByEventCategory().forEach(row ->
                Optional.ofNullable(summary.get("categoryCounts"))
                        .filter(m -> m instanceof Map)
                        .map(m -> (Map<String, Long>) m)
                        .ifPresentOrElse(
                                m -> m.put((String) row[0], (Long) row[1]),
                                () -> summary.put("categoryCounts", new HashMap<>(Map.of((String) row[0], (Long) row[1])))
                        )
        );

        if (merchantId != null) {
            summary.put("recentMetrics", metricService.getRecentMetrics(merchantId));
        }

        return summary;
    }

    public List<Report> getReports(String merchantId, String status) {
        boolean hasMerchant = merchantId != null;
        boolean hasStatus = status != null;

        if (hasMerchant && hasStatus) {
            return reportRepository.findByMerchantIdAndStatus(merchantId, status);
        } else if (hasMerchant) {
            return reportRepository.findByMerchantId(merchantId);
        } else if (hasStatus) {
            return reportRepository.findByStatus(status);
        }
        return reportRepository.findAll();
    }

    @Transactional
    public Report createReport(String reportType, String reportName, Map<String, Object> params,
                              String merchantId, String createdBy) {
        Report report = new Report();
        report.setReportType(reportType);
        report.setReportName(reportName);
        report.setMerchantId(merchantId);
        report.setCreatedBy(createdBy);
        report.setStatus("PENDING");
        report.setCreatedAt(Instant.now());

        try {
            report.setReportParams(objectMapper.writeValueAsString(params));
        } catch (Exception e) {
            report.setReportParams("{}");
        }

        log.info("analytics=report_created type={} name={} merchantId={}", reportType, reportName, merchantId);
        return reportRepository.save(report);
    }

    @Transactional
    public Report completeReport(Long reportId, Map<String, Object> reportData) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        report.setStatus("COMPLETED");
        report.setCompletedAt(Instant.now());

        try {
            report.setReportData(objectMapper.writeValueAsString(reportData));
        } catch (Exception e) {
            report.setReportData("{}");
        }

        return reportRepository.save(report);
    }

    public record DashboardMetrics(List<Map<String, Object>> recentMetrics, Map<String, Long> eventCounts, Map<String, Long> categoryCounts) {}

    public DashboardMetrics getMetricsSummary(String merchantId) {
        return new DashboardMetrics(
                metricService.getRecentMetrics(merchantId),
                getEventCounts(),
                getCategoryCounts()
        );
    }

    private Map<String, Long> getEventCounts() {
        Map<String, Long> counts = new HashMap<>();
        eventRepository.countByEventType().forEach(row -> counts.put((String) row[0], (Long) row[1]));
        return counts;
    }

    private Map<String, Long> getCategoryCounts() {
        Map<String, Long> counts = new HashMap<>();
        eventRepository.countByEventCategory().forEach(row -> counts.put((String) row[0], (Long) row[1]));
        return counts;
    }

    public Optional<Report> getReportById(Long id) {
        return reportRepository.findById(id);
    }

    public Map<String, Long> getRealTimeCounters() {
        Map<String, Long> counters = new HashMap<>();
        counterRepository.findAll().forEach(counter -> counters.put(counter.getCounterName(), counter.getCounterValue()));
        return counters;
    }

    public void updateKpi(String kpiName, Double value, String unit, String merchantId) {
        kpiService.updateKpi(kpiName, value, unit, merchantId);
    }

    public List<AnalyticsEvent> getEvents(String merchantId, String eventType, Instant start, Instant end, int limit) {
        return eventRepository.findByDateRange(start, end).stream().limit(limit).toList();
    }

    public Metric recordMetric(String metricName, String metricType, Double value,
                           String merchantId, String currency, String status, String period) {
        return metricService.recordMetric(metricName, metricType, value, merchantId, currency, status, period);
    }
}
