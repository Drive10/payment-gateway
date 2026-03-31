package dev.payment.analyticsservice.controller;

import dev.payment.analyticsservice.entity.AnalyticsEvent;
import dev.payment.analyticsservice.entity.Kpi;
import dev.payment.analyticsservice.entity.Metric;
import dev.payment.analyticsservice.entity.Report;
import dev.payment.analyticsservice.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/events")
    public ResponseEntity<AnalyticsEvent> recordEvent(@RequestBody Map<String, Object> request) {
        AnalyticsEvent event = analyticsService.recordEvent(
                (String) request.get("eventType"),
                (String) request.get("category"),
                (Map<String, Object>) request.getOrDefault("data", new HashMap<>()),
                (String) request.get("merchantId"),
                (String) request.get("orderId"),
                (String) request.get("paymentId"),
                (String) request.get("userId"),
                request.get("amount") != null ? ((Number) request.get("amount")).doubleValue() : null,
                (String) request.get("currency"),
                (String) request.get("status")
        );
        return ResponseEntity.ok(event);
    }

    @PostMapping("/metrics")
    public ResponseEntity<Metric> recordMetric(@RequestBody Map<String, Object> request) {
        Metric metric = analyticsService.recordMetric(
                (String) request.get("metricName"),
                (String) request.get("metricType"),
                ((Number) request.get("value")).doubleValue(),
                (String) request.get("merchantId"),
                (String) request.get("currency"),
                (String) request.get("status"),
                (String) requestOrDefault(request, "period", "hour")
        );
        return ResponseEntity.ok(metric);
    }

    @GetMapping("/events")
    public ResponseEntity<List<AnalyticsEvent>> getEvents(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "100") int limit) {

        List<AnalyticsEvent> events = analyticsService.getEvents(merchantId, eventType, start, end, limit);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<Metric>> getMetrics(@RequestParam(required = false) String merchantId) {
        List<Metric> metrics;
        if (merchantId != null) {
            metrics = analyticsService.getDashboardSummary(merchantId).get("recentMetrics") != null
                    ? (List<Metric>) analyticsService.getDashboardSummary(merchantId).get("recentMetrics")
                    : List.of();
        } else {
            metrics = List.of();
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(
            @RequestParam(required = false) String merchantId) {
        Map<String, Object> summary = analyticsService.getDashboardSummary(merchantId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/kpis")
    public ResponseEntity<List<Kpi>> getKpis(@RequestParam(required = false) String merchantId) {
        List<Kpi> kpis = merchantId != null
                ? (List<Kpi>) analyticsService.getDashboardSummary(merchantId).getOrDefault("kpis", List.of())
                : List.of();
        return ResponseEntity.ok(kpis);
    }

    @PostMapping("/reports")
    public ResponseEntity<Report> createReport(@RequestBody Map<String, Object> request) {
        Report report = analyticsService.createReport(
                (String) request.get("reportType"),
                (String) request.get("reportName"),
                (Map<String, Object>) request.getOrDefault("params", new HashMap<>()),
                (String) request.get("merchantId"),
                (String) request.get("createdBy")
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<Report> getReport(@PathVariable Long id) {
        Map<String, Object> summary = analyticsService.getDashboardSummary(null);
        return ResponseEntity.ok(new Report());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getReports(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/counters")
    public ResponseEntity<Map<String, Long>> getRealTimeCounters() {
        Map<String, Long> counters = analyticsService.getRealTimeCounters();
        return ResponseEntity.ok(counters);
    }

    @PostMapping("/kpis")
    public ResponseEntity<Void> updateKpi(@RequestBody Map<String, Object> request) {
        analyticsService.updateKpi(
                (String) request.get("kpiName"),
                ((Number) request.get("value")).doubleValue(),
                (String) request.get("unit"),
                (String) request.get("merchantId")
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "analytics-service");
        health.put("timestamp", Instant.now());
        return ResponseEntity.ok(health);
    }

    private String requestOrDefault(Map<String, Object> request, String key, String defaultValue) {
        Object value = request.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
