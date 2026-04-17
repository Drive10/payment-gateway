package dev.payment.paymentservice.strategy;

import dev.payment.paymentservice.domain.FeeConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CardFeeStrategy extends AbstractPaymentMethodFeeStrategy {
    
    private static final BigDecimal CARD_FEE_PERCENT = new BigDecimal("1.50");
    
    @Override
    protected BigDecimal getDefaultFeePercent() {
        return CARD_FEE_PERCENT;
    }
    
    @Override
    public String getPaymentMethod() {
        return "CARD";
    }
}
