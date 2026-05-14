package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findBySubscriptionId(String subscriptionId);
    List<Subscription> findByMerchantId(String merchantId);
    List<Subscription> findByCustomerEmail(String customerEmail);
    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);
    List<Subscription> findByMerchantIdAndStatus(String merchantId, Subscription.SubscriptionStatus status);
}
