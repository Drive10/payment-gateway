package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.ledger.LedgerAccount;
import dev.payment.paymentservice.payment.domain.enums.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    List<LedgerAccount> findByMerchantId(UUID merchantId);
    Optional<LedgerAccount> findByMerchantIdAndAccountType(UUID merchantId, LedgerAccountType accountType);
    Optional<LedgerAccount> findByAccountTypeAndMerchantIdIsNull(LedgerAccountType accountType);
}