package dev.payment.combinedservice.payment.strategy;

import dev.payment.combinedservice.payment.domain.FeeConfig;
import dev.payment.combinedservice.payment.dto.FeeCalculation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public abstract class AbstractPaymentMethodFeeStrategy implements PaymentMethodFeeStrategy {
    
    protected static final BigDecimal DEFAULT_GATEWAY_FEE_PERCENT = new BigDecimal("1.50");
    
    @Override
    public BigDecimal getGatewayFeePercent(FeeConfig config) {
        if (config != null && config.getGatewayFeePercent() != null) {
            return config.getGatewayFeePercent();
        }
        return getDefaultFeePercent();
    }
    
    protected abstract BigDecimal getDefaultFeePercent();
}
