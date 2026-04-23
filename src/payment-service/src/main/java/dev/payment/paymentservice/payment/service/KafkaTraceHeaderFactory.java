package dev.payment.paymentservice.payment.service;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class KafkaTraceHeaderFactory {

    private final Tracer tracer;
    private final Propagator propagator;

    public KafkaTraceHeaderFactory(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public Map<String, String> currentHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        if (tracer.currentSpan() == null) {
            return headers;
        }
        propagator.inject(tracer.currentSpan().context(), headers, Map::put);
        return headers;
    }
}
