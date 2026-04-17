package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.FeeConfig;
import dev.payment.paymentservice.dto.request.CreateFeeConfigRequest;
import dev.payment.paymentservice.repository.FeeConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FeeConfigService {

    private final FeeConfigRepository feeConfigRepository;

    public FeeConfigService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }

    @Transactional
    public FeeConfig createOrUpdateFeeConfig(CreateFeeConfigRequest request) {
        Optional<FeeConfig> existing = feeConfigRepository.findByMerchantId(request.merchantId());
        
        FeeConfig config;
        if (existing.isPresent()) {
            config = existing.get();
        } else {
            config = new FeeConfig();
            config.setMerchantId(request.merchantId());
        }

        if (request.pricingTier() != null) config.setPricingTier(request.pricingTier());
        if (request.platformFeePercent() != null) config.setPlatformFeePercent(request.platformFeePercent());
        if (request.platformFixedFee() != null) config.setPlatformFixedFee(request.platformFixedFee());
        if (request.gatewayFeePercent() != null) config.setGatewayFeePercent(request.gatewayFeePercent());
        if (request.gatewayFixedFee() != null) config.setGatewayFixedFee(request.gatewayFixedFee());
        if (request.volumeThreshold() != null) config.setVolumeThreshold(request.volumeThreshold());
        if (request.volumeDiscountPercent() != null) config.setVolumeDiscountPercent(request.volumeDiscountPercent());
        if (request.minFee() != null) config.setMinFee(request.minFee());
        if (request.maxFeePercent() != null) config.setMaxFeePercent(request.maxFeePercent());

        return feeConfigRepository.save(config);
    }

    public Optional<FeeConfig> getFeeConfig(UUID merchantId) {
        return feeConfigRepository.findByMerchantId(merchantId);
    }

    public List<FeeConfig> getAllFeeConfigs() {
        return feeConfigRepository.findAll();
    }

    @Transactional
    public void deactivateFeeConfig(UUID merchantId) {
        feeConfigRepository.findByMerchantId(merchantId).ifPresent(config -> {
            config.setActive(false);
            feeConfigRepository.save(config);
        });
    }
}
