package dev.payment.ledgerservice.service;

import dev.payment.common.events.PaymentEventMessage;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
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
    private final Tracer tracer;
    private final Propagator propagator;

    public PaymentEventListener(LedgerEventPostingService ledgerEventPostingService, Tracer tracer, Propagator propagator) {
        this.ledgerEventPostingService = ledgerEventPostingService;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Transactional
    @KafkaListener(topics = "${application.kafka.topic.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(ConsumerRecord<String, PaymentEventMessage> record) {
        PaymentEventMessage message = record.value();
        withTrace(record.headers(), message, () -> {
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
            ConsumerRecord<String, PaymentEventMessage> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        PaymentEventMessage message = record.value();
        withTrace(record.headers(), message, () -> log.error(
                "event=ledger_payment_event_dead_letter paymentId={} eventType={} topic={} version={}",
                message.paymentId(),
                message.eventType(),
                topic,
                message.eventVersion()
        ));
    }

    private void withTrace(Headers headers, PaymentEventMessage message, Runnable action) {
        Span.Builder builder = propagator.extract(headers, HEADER_GETTER);
        Span span = builder.name("ledger.payment.event").start();
        String previous = MDC.get("correlationId");
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            if (message.correlationId() != null && !message.correlationId().isBlank()) {
                MDC.put("correlationId", message.correlationId());
            }
            action.run();
        } finally {
            span.end();
            if (previous == null) {
                MDC.remove("correlationId");
            } else {
                MDC.put("correlationId", previous);
            }
        }
    }

    private static final Propagator.Getter<Headers> HEADER_GETTER = (carrier, key) -> {
        if (carrier == null || carrier.lastHeader(key) == null) {
            return null;
        }
        return new String(carrier.lastHeader(key).value(), java.nio.charset.StandardCharsets.UTF_8);
    };
}
