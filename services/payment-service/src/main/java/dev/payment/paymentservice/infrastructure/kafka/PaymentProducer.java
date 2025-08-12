package dev.payment.paymentservice.infrastructure.kafka;
import dev.payment.paymentservice.domain.Payment; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.stereotype.Component;
@Component public class PaymentProducer {
  private final KafkaTemplate<String,Object> kafka; public PaymentProducer(KafkaTemplate<String,Object> kafka){ this.kafka=kafka; }
  public void publishRequested(Payment p){ kafka.send("payments.requested", p.id(), p); }
}
