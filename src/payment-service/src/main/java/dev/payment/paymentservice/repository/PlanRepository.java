package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByMerchantId(String merchantId);
    List<Plan> findByMerchantIdAndStatus(String merchantId, String status);
}
