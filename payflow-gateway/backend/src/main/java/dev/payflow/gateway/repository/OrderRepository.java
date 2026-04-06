package dev.payflow.gateway.repository;

import dev.payflow.gateway.document.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByMerchantApiKey(String merchantApiKey);
    List<Order> findByPaymentLinkId(String paymentLinkId);
}
