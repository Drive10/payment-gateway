package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    List<AnalyticsEvent> findByEventType(String eventType);
    
    List<AnalyticsEvent> findByMerchantId(String merchantId);
    
    List<AnalyticsEvent> findByOrderId(String orderId);
    
    List<AnalyticsEvent> findByPaymentId(String paymentId);
    
    @Query("SELECT e FROM AnalyticsEvent e WHERE e.createdAt BETWEEN :start AND :end")
    List<AnalyticsEvent> findByDateRange(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    @Query("SELECT e.eventType, COUNT(e) FROM AnalyticsEvent e GROUP BY e.eventType")
    List<Object[]> countByEventType();
    
    @Query("SELECT e.eventCategory, COUNT(e) FROM AnalyticsEvent e GROUP BY e.eventCategory")
    List<Object[]> countByEventCategory();
}
