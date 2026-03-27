package dev.payment.ledgerservice.service;

import dev.payment.common.events.PaymentEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final LedgerEventPostingService ledgerEventPostingService;

    public PaymentEventListener(LedgerEventPostingService ledgerEventPostingService) {
        this.ledgerEventPostingService = ledgerEventPostingService;
    }

    @Transactional
    @KafkaListener(topics = "${application.kafka.topic.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(PaymentEventMessage message) {
        withCorrelation(message, () -> {
            switch (message.eventType()) {
                case "payment.captured", "payment.webhook.captured" -> ledgerEventPostingService.postCapture(message);
                case "payment.refunded", "payment.webhook.refund_processed" -> ledgerEventPostingService.postRefund(message);
                default -> log.debug("event=ledger_payment_event_skipped paymentId={} eventType={} version={}",
                        message.paymentId(), message.eventType(), message.eventVersion());
            }
        });
    }

    @KafkaListener(topics = "${application.kafka.topic.payment-events-dlt}", groupId = "${spring.kafka.consumer.group-id}.dlt")
    public void onDeadLetterEvent(
            PaymentEventMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        withCorrelation(message, () -> log.error(
                "event=ledger_payment_event_dead_letter paymentId={} eventType={} topic={} version={}",
                message.paymentId(),
                message.eventType(),
                topic,
                message.eventVersion()
        ));
    }

    private void withCorrelation(PaymentEventMessage message, Runnable action) {
        String previous = MDC.get("correlationId");
        try {
            if (message.correlationId() != null && !message.correlationId().isBlank()) {
                MDC.put("correlationId", message.correlationId());
            }
            action.run();
        } finally {
            if (previous == null) {
                MDC.remove("correlationId");
            } else {
                MDC.put("correlationId", previous);
            }
        }
    }
}
