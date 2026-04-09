package dev.payment.paymentservice.strategy;

import dev.payment.paymentservice.domain.FeeConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UpiFeeStrategy extends AbstractPaymentMethodFeeStrategy {
    
    private static final BigDecimal UPI_FEE_PERCENT = new BigDecimal("0.50");
    
    @Override
    protected BigDecimal getDefaultFeePercent() {
        return UPI_FEE_PERCENT;
    }
    
    @Override
    public String getPaymentMethod() {
        return "UPI";
    }
}
