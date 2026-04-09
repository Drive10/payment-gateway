package dev.payment.paymentservice.strategy;

import dev.payment.paymentservice.domain.FeeConfig;
import dev.payment.paymentservice.dto.FeeCalculation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public interface PaymentMethodFeeStrategy {
    BigDecimal getGatewayFeePercent(FeeConfig config);
    
    String getPaymentMethod();
}
