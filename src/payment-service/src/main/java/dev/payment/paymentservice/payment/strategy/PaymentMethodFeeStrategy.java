package dev.payment.paymentservice.payment.strategy;

import dev.payment.paymentservice.payment.domain.FeeConfig;
import dev.payment.paymentservice.payment.dto.FeeCalculation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public interface PaymentMethodFeeStrategy {
    BigDecimal getGatewayFeePercent(FeeConfig config);
    
    String getPaymentMethod();
}
