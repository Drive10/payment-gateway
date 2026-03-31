package dev.payment.common.idempotency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class IdempotencyHelper {

    private static final ConcurrentHashMap<String, IdempotentResult> results = new ConcurrentHashMap<>();

    public static <T> T execute(String idempotencyKey, Supplier<T> operation) {
        IdempotentResult existing = results.get(idempotencyKey);
        if (existing != null && !existing.isExpired()) {
            return existing.getResult();
        }

        T result = operation.get();
        results.put(idempotencyKey, new IdempotentResult(result, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)));
        return result;
    }

    public static <T> T executeWithLock(String idempotencyKey, Supplier<T> operation) {
        synchronized (idempotencyKey.intern()) {
            return execute(idempotencyKey, operation);
        }
    }

    public static boolean isProcessed(String idempotencyKey) {
        IdempotentResult result = results.get(idempotencyKey);
        return result != null && !result.isExpired();
    }

    public static void clearExpired() {
        long now = System.currentTimeMillis();
        results.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class IdempotentResult {
        private final Object result;
        private final long expiresAt;

        public IdempotentResult(Object result, long expiresAt) {
            this.result = result;
            this.expiresAt = expiresAt;
        }

        @SuppressWarnings("unchecked")
        public <T> T getResult() {
            return (T) result;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
