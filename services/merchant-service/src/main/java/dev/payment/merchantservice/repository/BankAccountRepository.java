package dev.payment.merchantservice.repository;

import dev.payment.merchantservice.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findByMerchantId(UUID merchantId);
    
    Optional<BankAccount> findByMerchantIdAndIsDefaultTrue(UUID merchantId);
}
