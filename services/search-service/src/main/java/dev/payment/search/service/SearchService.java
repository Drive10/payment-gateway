package dev.payment.search.service;

import dev.payment.search.document.PaymentDocument;
import dev.payment.search.repository.PaymentSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final PaymentSearchRepository paymentRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public SearchService(PaymentSearchRepository paymentRepository, 
                       ElasticsearchOperations elasticsearchOperations) {
        this.paymentRepository = paymentRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void log(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }

    /**
     * Search payments with natural language query
     * Example: "Find all failed payments last 24h"
     */
    public Map<String, Object> searchPayments(String query, int page, int size) {
        log("INFO", "[Search] Query: '" + query + "', page: " + page + ", size: " + size);
        
        Map<String, Object> result = new HashMap<>();
        
        // Parse natural language query
        String status = parseStatusFromQuery(query);
        Instant startDate = parseDateFromQuery(query);
        
        Criteria criteria = new Criteria();
        
        if (status != null) {
            criteria = criteria.and("status").is(status);
            log("INFO", "[Search] Filtering by status: " + status);
        }
        
        if (startDate != null) {
            criteria = criteria.and("createdAt").greaterThanEqual(startDate);
            log("INFO", "[Search] Filtering from date: " + startDate);
        }
        
        if (query.contains("amount")) {
            Double minAmount = extractAmountFromQuery(query);
            if (minAmount != null) {
                criteria = criteria.and("amount").greaterThanEqual(minAmount);
            }
        }
        
        Query searchQuery = new CriteriaQuery(criteria);
        searchQuery.setPageable(org.springframework.data.domain.PageRequest.of(page, size));
        
        SearchHits<PaymentDocument> searchHits = elasticsearchOperations.search(
            searchQuery, PaymentDocument.class
        );
        
        List<Map<String, Object>> payments = searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(this::paymentToMap)
            .collect(Collectors.toList());
        
        result.put("payments", payments);
        result.put("totalCount", searchHits.getTotalHits());
        result.put("page", page);
        result.put("pageSize", size);
        result.put("hasMore", (page + 1) * size < searchHits.getTotalHits());
        
        return result;
    }

    private String parseStatusFromQuery(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("failed") || lowerQuery.contains("failure")) {
            return "FAILED";
        }
        if (lowerQuery.contains("completed") || lowerQuery.contains("success")) {
            return "COMPLETED";
        }
        if (lowerQuery.contains("pending")) {
            return "PENDING";
        }
        if (lowerQuery.contains("refund")) {
            return "REFUNDED";
        }
        
        return null;
    }

    private Instant parseDateFromQuery(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("last 24h") || lowerQuery.contains("last 24")) {
            return Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (lowerQuery.contains("last 7 days") || lowerQuery.contains("last week")) {
            return Instant.now().minus(7, ChronoUnit.DAYS);
        }
        if (lowerQuery.contains("last 30 days") || lowerQuery.contains("last month")) {
            return Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (lowerQuery.contains("today")) {
            return Instant.now().truncatedTo(ChronoUnit.DAYS);
        }
        if (lowerQuery.contains("yesterday")) {
            return Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        }
        
        return null;
    }

    private Double extractAmountFromQuery(String query) {
        try {
            String[] words = query.split("\\s+");
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equalsIgnoreCase("amount") && words[i + 1].contains(">")) {
                    String amountStr = words[i + 1].replaceAll("[^0-9.]", "");
                    return Double.parseDouble(amountStr);
                }
            }
        } catch (Exception e) {
            log("WARN", "Failed to parse amount from query: " + query);
        }
        return null;
    }

    public PaymentDocument indexPayment(PaymentDocument payment) {
        payment.setUpdatedAt(Instant.now());
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(Instant.now());
        }
        return paymentRepository.save(payment);
    }

    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
    }

    public Optional<PaymentDocument> getPayment(String id) {
        return paymentRepository.findById(id);
    }

    public List<PaymentDocument> getAllPayments(int page, int size) {
        return paymentRepository.findAll(
            org.springframework.data.domain.PageRequest.of(page, size)
        ).getContent();
    }

    private Map<String, Object> paymentToMap(PaymentDocument payment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", payment.getId());
        map.put("orderId", payment.getOrderId());
        map.put("amount", payment.getAmount());
        map.put("currency", payment.getCurrency());
        map.put("paymentMethod", payment.getPaymentMethod());
        map.put("provider", payment.getProvider());
        map.put("status", payment.getStatus());
        map.put("simulated", payment.getSimulated());
        map.put("failureReason", payment.getFailureReason());
        map.put("createdAt", payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
        return map;
    }
}
