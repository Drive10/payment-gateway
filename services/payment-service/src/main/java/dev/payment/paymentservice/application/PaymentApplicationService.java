package dev.payment.paymentservice.application;
import dev.payment.common.dto.PaymentDTO; import dev.payment.paymentservice.domain.Payment; import dev.payment.paymentservice.domain.PaymentRepository;
import dev.payment.paymentservice.infrastructure.kafka.PaymentProducer;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.*; @Service public class PaymentApplicationService {
  private final PaymentRepository repo; private final PaymentProducer producer;
  public PaymentApplicationService(PaymentRepository repo, PaymentProducer producer){ this.repo=repo; this.producer=producer; }
  @Transactional public Payment create(PaymentDTO dto){
    Payment p = new Payment(UUID.randomUUID().toString(), dto.orderId(), dto.customerId(), dto.currency(), dto.amount(), "REQUESTED");
    repo.save(p); producer.publishRequested(p); return p;
  }
  public Optional<Payment> findById(String id){ return repo.findById(id); }
}
