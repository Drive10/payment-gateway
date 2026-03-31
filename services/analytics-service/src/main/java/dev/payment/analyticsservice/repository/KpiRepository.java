package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface KpiRepository extends JpaRepository<Kpi, Long> {

    Optional<Kpi> findByKpiName(String kpiName);
    
    Optional<Kpi> findByKpiNameAndDimensionMerchantId(String kpiName, String merchantId);
    
    List<Kpi> findByDimensionMerchantId(String merchantId);
}
