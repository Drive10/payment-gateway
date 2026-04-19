package dev.payment.common.util;

/**
 * Utility class for waiting for dependent services to be ready.
 * This class has no Spring dependencies and can be used anywhere.
 */
public class WaitUtil {

    private static final int MAX_ATTEMPTS = 30;
    private static final int SLEEP_DURATION_SECONDS = 2;

    /**
     * Waits for a host and port to become available.
     *
     * @param host        the hostname or IP address
     * @param port        the port number
     * @param serviceName a descriptive name for the service (used in logging)
     * @throws IllegalStateException if the service doesn't become available within the timeout
     */
    public static void waitForHost(String host, int port, String serviceName) {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                System.out.println("✓ " + serviceName + " is ready at " + host + ":" + port);
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    System.err.println("✗ Failed to connect to " + serviceName + " after " + MAX_ATTEMPTS + " attempts");
                    throw new IllegalStateException(serviceName + " is not available", e);
                }
                System.out.println("⏳ Waiting for " + serviceName + " (" + attempt + "/" + MAX_ATTEMPTS + ")...");
                try {
                    Thread.sleep(SLEEP_DURATION_SECONDS * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for " + serviceName, ie);
                }
            }
        }
    }

    /**
     * Validates that required environment variables are set.
     *
     * @param requiredVars array of environment variable names that must be set
     * @throws IllegalStateException if any required variable is missing or empty
     */
    public static void validateRequiredEnvVars(String... requiredVars) {
        for (String var : requiredVars) {
            String value = System.getenv(var);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("Required environment variable '" + var + "' is not set");
            }
        }
    }
}