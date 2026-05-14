package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, UUID> {
    List<SavedPaymentMethod> findByCustomerEmail(String customerEmail);
    Optional<SavedPaymentMethod> findByToken(String token);
    List<SavedPaymentMethod> findByCustomerEmailAndType(String customerEmail, String type);
}
