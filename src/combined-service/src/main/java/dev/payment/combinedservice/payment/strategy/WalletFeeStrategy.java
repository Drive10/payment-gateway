package dev.payment.combinedservice.payment.strategy;

import dev.payment.combinedservice.payment.domain.FeeConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WalletFeeStrategy extends AbstractPaymentMethodFeeStrategy {
    
    private static final BigDecimal WALLET_FEE_PERCENT = new BigDecimal("1.50");
    
    @Override
    protected BigDecimal getDefaultFeePercent() {
        return WALLET_FEE_PERCENT;
    }
    
    @Override
    public String getPaymentMethod() {
        return "WALLET";
    }
}
