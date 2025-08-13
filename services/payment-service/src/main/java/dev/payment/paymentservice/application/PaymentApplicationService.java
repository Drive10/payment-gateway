package dev.payment.paymentservice.application;
import dev.payment.common.dto.PaymentDTO; import dev.payment.paymentservice.domain.Payment; import dev.payment.paymentservice.domain.PaymentRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.*; @Service public class PaymentApplicationService {
  private final PaymentRepository repo;
  public PaymentApplicationService(PaymentRepository repo){ this.repo=repo; }
  @Transactional public Payment create(PaymentDTO dto){
    Payment p = new Payment(UUID.randomUUID().toString(), dto.orderId(), dto.customerId(), dto.currency(), dto.amount(), "REQUESTED");
    repo.save(p); return p;
  }
  public Optional<Payment> findById(String id){ return repo.findById(id); }
}
