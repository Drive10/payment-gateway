package dev.payment.paymentservice.strategy;

import dev.payment.paymentservice.domain.FeeConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultFeeStrategy extends AbstractPaymentMethodFeeStrategy {
    
    private static final BigDecimal DEFAULT_FEE_PERCENT = new BigDecimal("1.50");
    
    @Override
    protected BigDecimal getDefaultFeePercent() {
        return DEFAULT_FEE_PERCENT;
    }
    
    @Override
    public String getPaymentMethod() {
        return "DEFAULT";
    }
}
