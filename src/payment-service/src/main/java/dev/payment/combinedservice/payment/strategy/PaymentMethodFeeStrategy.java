package dev.payment.combinedservice.payment.strategy;

import dev.payment.combinedservice.payment.domain.FeeConfig;
import dev.payment.combinedservice.payment.dto.FeeCalculation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public interface PaymentMethodFeeStrategy {
    BigDecimal getGatewayFeePercent(FeeConfig config);
    
    String getPaymentMethod();
}
