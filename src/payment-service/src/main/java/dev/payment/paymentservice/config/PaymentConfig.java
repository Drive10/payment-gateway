package dev.payment.paymentservice.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {
    private Set<String> supportedCurrencies = new HashSet<>(Set.of("INR", "USD", "EUR", "GBP"));
    private Map<String, CurrencyConfig> currencies = new HashMap<>();

    public PaymentConfig() {
        currencies.put("INR", new CurrencyConfig("Indian Rupee", "₹", 2));
        currencies.put("USD", new CurrencyConfig("US Dollar", "$", 2));
        currencies.put("EUR", new CurrencyConfig("Euro", "€", 2));
        currencies.put("GBP", new CurrencyConfig("British Pound", "£", 2));
    }

    public Set<String> getSupportedCurrencies() {
        return supportedCurrencies;
    }

    public void setSupportedCurrencies(Set<String> supportedCurrencies) {
        this.supportedCurrencies = supportedCurrencies;
    }

    public boolean isCurrencySupported(String currency) {
        return supportedCurrencies.contains(currency.toUpperCase());
    }

    public CurrencyConfig getCurrencyConfig(String currency) {
        return currencies.get(currency.toUpperCase());
    }

    @Data
    @AllArgsConstructor
    public static class CurrencyConfig {
        private String name;
        private String symbol;
        private int decimalPlaces;
    }
}