package dev.payment.analyticsservice.controller;

import dev.payment.analyticsservice.entity.AnalyticsEvent;
import dev.payment.analyticsservice.entity.Kpi;
import dev.payment.analyticsservice.entity.Metric;
import dev.payment.analyticsservice.entity.Report;
import dev.payment.analyticsservice.service.AnalyticsService;
import dev.payment.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
    public ResponseEntity<ApiResponse<AnalyticsEvent>> recordEvent(@RequestBody Map<String, Object> request) {
        validateEventRequest(request);

        AnalyticsEvent event = analyticsService.recordEvent(
                getString(request, "eventType"),
                getString(request, "category"),
                getMap(request, "data"),
                getString(request, "merchantId"),
                getString(request, "orderId"),
                getString(request, "paymentId"),
                getString(request, "userId"),
                getDouble(request, "amount"),
                getString(request, "currency"),
                getString(request, "status")
        );

        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping("/metrics")
    public ResponseEntity<ApiResponse<Metric>> recordMetric(@RequestBody Map<String, Object> request) {
        validateMetricRequest(request);

        Metric metric = analyticsService.recordMetric(
                getString(request, "metricName"),
                getString(request, "metricType"),
                getDoubleRequired(request, "value"),
                getString(request, "merchantId"),
                getString(request, "currency"),
                getString(request, "status"),
                getStringOrDefault(request, "period", "hour")
        );

        return ResponseEntity.ok(ApiResponse.success(metric));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<AnalyticsEvent>>> getEvents(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end,
            @RequestParam(defaultValue = "100") @Positive int limit) {

        List<AnalyticsEvent> events = analyticsService.getEvents(merchantId, eventType, start, end, limit);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<List<Metric>>> getMetrics(
            @RequestParam(required = false) String merchantId) {

        List<Metric> metrics;
        if (merchantId != null) {
            Map<String, Object> summary = analyticsService.getDashboardSummary(merchantId);
            metrics = extractMetrics(summary);
        } else {
            metrics = List.of();
        }

        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(
            @RequestParam(required = false) String merchantId) {

        Map<String, Object> summary = analyticsService.getDashboardSummary(merchantId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<List<Kpi>>> getKpis(
            @RequestParam(required = false) String merchantId) {

        List<Kpi> kpis;
        if (merchantId != null) {
            Map<String, Object> summary = analyticsService.getDashboardSummary(merchantId);
            kpis = extractKpis(summary);
        } else {
            kpis = List.of();
        }

        return ResponseEntity.ok(ApiResponse.success(kpis));
    }

    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<Report>> createReport(@RequestBody Map<String, Object> request) {
        validateReportRequest(request);

        Report report = analyticsService.createReport(
                getStringRequired(request, "reportType"),
                getStringRequired(request, "reportName"),
                getMap(request, "params"),
                getString(request, "merchantId"),
                getString(request, "createdBy")
        );

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Report>> getReport(@PathVariable Long id) {
        Report report = analyticsService.getReportById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Report>>> getReports(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status) {

        List<Report> reports = analyticsService.getReports(merchantId, status);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/counters")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRealTimeCounters() {
        Map<String, Long> counters = analyticsService.getRealTimeCounters();
        return ResponseEntity.ok(ApiResponse.success(counters));
    }

    @PostMapping("/kpis")
    public ResponseEntity<ApiResponse<Void>> updateKpi(@RequestBody Map<String, Object> request) {
        validateKpiRequest(request);

        analyticsService.updateKpi(
                getStringRequired(request, "kpiName"),
                getDoubleRequired(request, "value"),
                getString(request, "unit"),
                getString(request, "merchantId")
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "analytics-service");
        health.put("timestamp", Instant.now());
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    private void validateEventRequest(Map<String, Object> request) {
        if (request.get("eventType") == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (request.get("category") == null) {
            throw new IllegalArgumentException("category is required");
        }
    }

    private void validateMetricRequest(Map<String, Object> request) {
        if (request.get("metricName") == null) {
            throw new IllegalArgumentException("metricName is required");
        }
        if (request.get("metricType") == null) {
            throw new IllegalArgumentException("metricType is required");
        }
        if (request.get("value") == null) {
            throw new IllegalArgumentException("value is required");
        }
    }

    private void validateReportRequest(Map<String, Object> request) {
        if (request.get("reportType") == null) {
            throw new IllegalArgumentException("reportType is required");
        }
        if (request.get("reportName") == null) {
            throw new IllegalArgumentException("reportName is required");
        }
    }

    private void validateKpiRequest(Map<String, Object> request) {
        if (request.get("kpiName") == null) {
            throw new IllegalArgumentException("kpiName is required");
        }
        if (request.get("value") == null) {
            throw new IllegalArgumentException("value is required");
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getStringRequired(Map<String, Object> map, String key) {
        String value = getString(map, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : new HashMap<>();
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a valid number");
        }
    }

    private Double getDoubleRequired(Map<String, Object> map, String key) {
        Double value = getDouble(map, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private List<Metric> extractMetrics(Map<String, Object> summary) {
        Object metrics = summary.get("recentMetrics");
        if (metrics instanceof List) {
            return (List<Metric>) metrics;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Kpi> extractKpis(Map<String, Object> summary) {
        Object kpis = summary.get("kpis");
        if (kpis instanceof List) {
            return (List<Kpi>) kpis;
        }
        return List.of();
    }
}
