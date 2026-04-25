package dev.payment.paymentservice.dto;

import dev.payment.paymentservice.config.PaymentConfig;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SupportedCurrencyValidator implements ConstraintValidator<SupportedCurrency, String> {
    private final PaymentConfig paymentConfig;

    @Override
    public boolean isValid(String currency, ConstraintValidatorContext context) {
        if (currency == null) {
            return true;
        }
        return paymentConfig.isCurrencySupported(currency);
    }
}