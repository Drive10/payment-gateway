package dev.payment.common.util;

/**
 * Utility class for waiting for dependent services to be ready.
 * This class has no Spring dependencies and can be used anywhere.
 */
public class WaitUtil {

    private static final int MAX_ATTEMPTS = 30;
    private static final int SLEEP_DURATION_SECONDS = 2;

    public static void waitForHost(String host, int port, String serviceName) {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    throw new IllegalStateException(serviceName + " is not available", e);
                }
                try {
                    Thread.sleep(SLEEP_DURATION_SECONDS * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for " + serviceName, ie);
                }
            }
        }
    }

    public static void validateRequiredEnvVars(String... requiredVars) {
        for (String var : requiredVars) {
            String value = System.getenv(var);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("Required environment variable '" + var + "' is not set");
            }
        }
    }
}