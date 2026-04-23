package dev.payment.paymentservice.payment.strategy;

import dev.payment.paymentservice.payment.domain.FeeConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NetbankingFeeStrategy extends AbstractPaymentMethodFeeStrategy {
    
    private static final BigDecimal NETBANKING_FEE_PERCENT = new BigDecimal("1.00");
    
    @Override
    protected BigDecimal getDefaultFeePercent() {
        return NETBANKING_FEE_PERCENT;
    }
    
    @Override
    public String getPaymentMethod() {
        return "NETBANKING";
    }
}
