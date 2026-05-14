package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByMerchantId(String merchantId);
    List<Invoice> findByCustomerEmail(String customerEmail);
    List<Invoice> findByMerchantIdAndStatus(String merchantId, Invoice.InvoiceStatus status);
    List<Invoice> findBySubscriptionId(String subscriptionId);
}
