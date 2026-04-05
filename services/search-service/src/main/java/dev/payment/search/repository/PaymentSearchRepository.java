package dev.payment.search.repository;

import dev.payment.search.document.PaymentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentSearchRepository extends ElasticsearchRepository<PaymentDocument, String> {
    
    List<PaymentDocument> findByStatus(String status);
    
    List<PaymentDocument> findByPaymentMethod(String paymentMethod);
    
    List<PaymentDocument> findByStatusAndPaymentMethod(String status, String paymentMethod);
    
    List<PaymentDocument> findByOrderIdContaining(String orderId);
}
