package dev.payment.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "payflow")
public class PayFlowProperties {

    private ServiceDefaults service = new ServiceDefaults();
    private Database database = new Database();
    private Redis redis = new Redis();
    private Kafka kafka = new Kafka();

    public ServiceDefaults getService() {
        return service;
    }

    public void setService(ServiceDefaults service) {
        this.service = service;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public static class ServiceDefaults {
        private Map<String, String> urls = new HashMap<>();

        public Map<String, String> getUrls() {
            return urls;
        }

        public void setUrls(Map<String, String> urls) {
            this.urls = urls;
        }

        public String getUrl(String serviceName) {
            return urls.getOrDefault(serviceName, "http://" + serviceName + ":808" + getPort(serviceName));
        }

        private int getPort(String serviceName) {
            return switch (serviceName.toLowerCase()) {
                case "api-gateway" -> 80;
                case "auth-service" -> 81;
                case "order-service" -> 82;
                case "payment-service" -> 83;
                case "notification-service" -> 84;
                case "simulator-service" -> 86;
                case "analytics-service" -> 89;
                case "audit-service" -> 90;
                default -> 80;
            };
        }
    }

    public static class Database {
        private String host = "localhost";
        private int port = 5432;
        private String username = "payflow";
        private String password = "payflow";
        private String name = "payflow";

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, name);
        }
    }

    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Kafka {
        private String host = "localhost";
        private int port = 9092;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getBootstrapServers() {
            return host + ":" + port;
        }
    }
}