package dev.payflow.gateway.repository;

import dev.payflow.gateway.document.PaymentLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentLinkRepository extends MongoRepository<PaymentLink, String> {
    Optional<PaymentLink> findByUuid(String uuid);
    List<PaymentLink> findByMerchantApiKey(String merchantApiKey);
    boolean existsByUuid(String uuid);
}
