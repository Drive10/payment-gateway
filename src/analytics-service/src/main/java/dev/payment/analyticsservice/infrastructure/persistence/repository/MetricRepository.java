package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    Optional<Metric> findByMetricNameAndDimensionMerchantIdAndPeriodStart(
        String metricName,
        String dimensionMerchantId,
        Instant periodStart
    );
    
    List<Metric> findByMetricName(String metricName);
    
    List<Metric> findByDimensionMerchantId(String merchantId);
    
    @Query("SELECT m FROM Metric m WHERE m.periodStart BETWEEN :start AND :end")
    List<Metric> findByPeriodRange(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    @Query("SELECT m FROM Metric m WHERE m.metricName = :name AND m.dimensionPeriod = :period")
    List<Metric> findByMetricNameAndPeriod(
        @Param("name") String metricName,
        @Param("period") String period
    );
}
