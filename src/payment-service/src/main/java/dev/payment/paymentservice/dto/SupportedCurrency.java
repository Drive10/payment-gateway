package dev.payment.paymentservice.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SupportedCurrencyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedCurrency {
    String message() default "Currency must be a supported currency code (INR, USD, EUR, GBP)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}