package dev.payment.paymentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fee_configs")
public class FeeConfig {
    private static final BigDecimal VOLUME_THRESHOLD = new BigDecimal("100000");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", unique = true, nullable = false)
    private UUID merchantId;

    @Column(name = "pricing_tier", nullable = false, length = 32)
    private String pricingTier = "STANDARD";

    @Column(name = "platform_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformFeePercent = new BigDecimal("2.00");

    @Column(name = "platform_fixed_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal platformFixedFee = BigDecimal.ZERO;

    @Column(name = "gateway_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gatewayFeePercent = new BigDecimal("1.50");

    @Column(name = "gateway_fixed_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal gatewayFixedFee = new BigDecimal("2.00");

    @Column(name = "volume_threshold", precision = 19, scale = 2)
    private BigDecimal volumeThreshold = VOLUME_THRESHOLD;

    @Column(name = "volume_discount_percent", precision = 5, scale = 2)
    private BigDecimal volumeDiscountPercent = new BigDecimal("0.25");

    @Column(name = "min_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal minFee = new BigDecimal("1.00");

    @Column(name = "max_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxFeePercent = new BigDecimal("5.00");

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public String getPricingTier() { return pricingTier; }
    public void setPricingTier(String pricingTier) { this.pricingTier = pricingTier; }

    public BigDecimal getPlatformFeePercent() { return platformFeePercent; }
    public void setPlatformFeePercent(BigDecimal platformFeePercent) { this.platformFeePercent = platformFeePercent; }

    public BigDecimal getPlatformFixedFee() { return platformFixedFee; }
    public void setPlatformFixedFee(BigDecimal platformFixedFee) { this.platformFixedFee = platformFixedFee; }

    public BigDecimal getGatewayFeePercent() { return gatewayFeePercent; }
    public void setGatewayFeePercent(BigDecimal gatewayFeePercent) { this.gatewayFeePercent = gatewayFeePercent; }

    public BigDecimal getGatewayFixedFee() { return gatewayFixedFee; }
    public void setGatewayFixedFee(BigDecimal gatewayFixedFee) { this.gatewayFixedFee = gatewayFixedFee; }

    public BigDecimal getVolumeThreshold() { return volumeThreshold; }
    public void setVolumeThreshold(BigDecimal volumeThreshold) { this.volumeThreshold = volumeThreshold; }

    public BigDecimal getVolumeDiscountPercent() { return volumeDiscountPercent; }
    public void setVolumeDiscountPercent(BigDecimal volumeDiscountPercent) { this.volumeDiscountPercent = volumeDiscountPercent; }

    public BigDecimal getMinFee() { return minFee; }
    public void setMinFee(BigDecimal minFee) { this.minFee = minFee; }

    public BigDecimal getMaxFeePercent() { return maxFeePercent; }
    public void setMaxFeePercent(BigDecimal maxFeePercent) { this.maxFeePercent = maxFeePercent; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
