package dev.payment.common.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ResilienceWrapper {

    private static final Logger log = LoggerFactory.getLogger(ResilienceWrapper.class);

    public <T> T executeWithCircuitBreaker(Supplier<T> supplier, String name, CircuitBreaker circuitBreaker) {
        return CircuitBreaker.decorateSupplier(circuitBreaker, supplier).get();
    }

    public <T> T executeWithRetry(Supplier<T> supplier, String name, Retry retry) {
        return Retry.decorateSupplier(retry, supplier).get();
    }

    public <T> T executeWithFallback(Supplier<T> supplier, T fallbackValue) {
        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is OPEN, returning fallback value");
            return fallbackValue;
        } catch (Exception e) {
            log.error("Error executing supplier, returning fallback: {}", e.getMessage());
            return fallbackValue;
        }
    }

    public CircuitBreaker.State getCircuitState(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getState();
    }

    public void recordSuccess(CircuitBreaker circuitBreaker) {
        circuitBreaker.record();
    }

    public void recordFailure(CircuitBreaker circuitBreaker, Throwable throwable) {
        circuitBreaker.recordFailure(throwable);
    }
}
