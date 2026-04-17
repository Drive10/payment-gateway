package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.FeeConfig;
import dev.payment.paymentservice.dto.FeeCalculation;
import dev.payment.paymentservice.repository.FeeConfigRepository;
import dev.payment.paymentservice.strategy.DefaultFeeStrategy;
import dev.payment.paymentservice.strategy.PaymentMethodFeeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FeeEngine {
    private static final BigDecimal FEE_DIVISOR = new BigDecimal("100");

    private static final Logger log = LoggerFactory.getLogger(FeeEngine.class);

    private static final BigDecimal DEFAULT_PLATFORM_FEE_PERCENT = new BigDecimal("2.00");
    private static final BigDecimal DEFAULT_PLATFORM_FIXED = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_GATEWAY_FIXED = new BigDecimal("2.00");
    private static final BigDecimal DEFAULT_MIN_FEE = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_MAX_FEE_PERCENT = new BigDecimal("5.00");

    private final FeeConfigRepository feeConfigRepository;
    private final Map<String, PaymentMethodFeeStrategy> feeStrategies;

    public FeeEngine(FeeConfigRepository feeConfigRepository, List<PaymentMethodFeeStrategy> strategies) {
        this.feeConfigRepository = feeConfigRepository;
        this.feeStrategies = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getPaymentMethod().toUpperCase(),
                        Function.identity()
                ));
    }

    public FeeCalculation calculateFees(BigDecimal amount, String pricingTier, String paymentMethod) {
        return calculateFees(null, amount, pricingTier, paymentMethod);
    }

    public FeeCalculation calculateFees(UUID merchantId, BigDecimal amount, String pricingTier, String paymentMethod) {
        BigDecimal platformPercent = DEFAULT_PLATFORM_FEE_PERCENT;
        BigDecimal platformFixed = DEFAULT_PLATFORM_FIXED;
        BigDecimal gatewayFixed = DEFAULT_GATEWAY_FIXED;
        BigDecimal minFee = DEFAULT_MIN_FEE;
        BigDecimal maxFeePercent = DEFAULT_MAX_FEE_PERCENT;
        BigDecimal volumeDiscount = BigDecimal.ZERO;

        Optional<FeeConfig> config = feeConfigRepository.findByMerchantIdAndActiveTrue(merchantId);
        if (config.isPresent()) {
            FeeConfig fc = config.get();
            platformPercent = fc.getPlatformFeePercent();
            platformFixed = fc.getPlatformFixedFee();
            gatewayFixed = fc.getGatewayFixedFee();
            minFee = fc.getMinFee();
            maxFeePercent = fc.getMaxFeePercent();

            if (fc.getVolumeThreshold() != null && fc.getVolumeDiscountPercent() != null) {
                volumeDiscount = fc.getVolumeDiscountPercent();
            }
        }

        BigDecimal gatewayPercent = getStrategy(paymentMethod).getGatewayFeePercent(config.orElse(null));

        BigDecimal effectivePlatformPercent = platformPercent;
        if (volumeDiscount.compareTo(BigDecimal.ZERO) > 0) {
            effectivePlatformPercent = platformPercent.subtract(volumeDiscount);
            if (effectivePlatformPercent.compareTo(BigDecimal.ZERO) < 0) {
                effectivePlatformPercent = BigDecimal.ZERO;
            }
        }

        BigDecimal platformFee = calculateFee(amount, effectivePlatformPercent, platformFixed, minFee, maxFeePercent);
        BigDecimal gatewayFee = calculateFee(amount, gatewayPercent, gatewayFixed, minFee, maxFeePercent);

        BigDecimal totalFee = platformFee.add(gatewayFee);
        BigDecimal netAmount = amount.subtract(totalFee);

        return new FeeCalculation(
                amount,
                platformFee,
                gatewayFee,
                totalFee,
                netAmount,
                effectivePlatformPercent,
                gatewayPercent
        );
    }

    public FeeCalculation calculateRefundFees(BigDecimal originalAmount, BigDecimal refundAmount, String pricingTier) {
        return calculateRefundFees(null, originalAmount, refundAmount, pricingTier);
    }

    public FeeCalculation calculateRefundFees(UUID merchantId, BigDecimal originalAmount, BigDecimal refundAmount, String pricingTier) {
        BigDecimal platformPercent = DEFAULT_PLATFORM_FEE_PERCENT;
        
        Optional<FeeConfig> config = feeConfigRepository.findByMerchantIdAndActiveTrue(merchantId);
        if (config.isPresent()) {
            platformPercent = config.get().getPlatformFeePercent();
        }

        BigDecimal refundFee = refundAmount.multiply(platformPercent)
                .divide(FEE_DIVISOR, 2, RoundingMode.HALF_UP);

        return new FeeCalculation(
                refundAmount,
                refundFee,
                BigDecimal.ZERO,
                refundFee,
                refundAmount.subtract(refundFee),
                platformPercent,
                BigDecimal.ZERO
        );
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal percent, BigDecimal fixed, BigDecimal minFee, BigDecimal maxFeePercent) {
        BigDecimal percentFee = amount.multiply(percent)
                .divide(FEE_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal totalFee = percentFee.add(fixed);

        BigDecimal maxFee = amount.multiply(maxFeePercent)
                .divide(FEE_DIVISOR, 2, RoundingMode.HALF_UP);

        if (totalFee.compareTo(minFee) < 0) {
            return minFee;
        } else if (totalFee.compareTo(maxFee) > 0) {
            return maxFee;
        }
        return totalFee.setScale(2, RoundingMode.HALF_UP);
    }

    private PaymentMethodFeeStrategy getStrategy(String paymentMethod) {
        if (paymentMethod == null) {
            return feeStrategies.getOrDefault("DEFAULT", new DefaultFeeStrategy());
        }
        return feeStrategies.getOrDefault(paymentMethod.toUpperCase(), new DefaultFeeStrategy());
    }
}
