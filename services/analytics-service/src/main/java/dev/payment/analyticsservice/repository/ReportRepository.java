package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(String status);
    
    List<Report> findByMerchantId(String merchantId);
    
    List<Report> findByReportType(String reportType);
    
    List<Report> findByCreatedBy(String createdBy);
    
    List<Report> findByMerchantIdAndStatus(String merchantId, String status);
}
