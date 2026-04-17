package dev.payment.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application")
public class ServiceConfig {

    private final Simulator simulator = new Simulator();
    private final Webhook webhook = new Webhook();
    private final Order order = new Order();
    private final Auth auth = new Auth();

    public Simulator getSimulator() {
        return simulator;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public Order getOrder() {
        return order;
    }

    public Auth getAuth() {
        return auth;
    }

    public static class Simulator {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Webhook {
        private String serviceUrl;
        private int maxRetries = 3;
        private int timeoutMs = 5000;
        private Razorpay razorpay = new Razorpay();

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Razorpay getRazorpay() {
            return razorpay;
        }

        public static class Razorpay {
            private String secret;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }
        }
    }

    public static class Order {
        private String serviceUrl;

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }
    }

    public static class Auth {
        private String serviceUrl;

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }
    }
}
