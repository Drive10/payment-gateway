package dev.payment.paymentservice.domain;

import dev.payment.paymentservice.domain.enums.PaymentMethod;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true, cascade = {})
    @JoinColumn(name = "order_id", nullable = true, insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Order order;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "provider_order_id", nullable = false, unique = true, length = 64)
    private String providerOrderId;

    @Column(name = "provider_payment_id", unique = true, length = 64)
    private String providerPaymentId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 32)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_mode", nullable = false, length = 16)
    private TransactionMode transactionMode;

    @Column(name = "simulated", nullable = false)
    private boolean simulated;

    @Column(name = "provider_signature", length = 255)
    private String providerSignature;

    @Column(name = "checkout_url", nullable = false, length = 255)
    private String checkoutUrl;

    @Column(length = 255)
    private String notes;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "platform_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "gateway_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(name = "pricing_tier", length = 32)
    private String pricingTier = "STANDARD";

    @Version
    @Column(nullable = false)
    private long version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
        if (order != null) {
            this.orderId = order.getId();
        }
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public void setProviderOrderId(String providerOrderId) {
        this.providerOrderId = providerOrderId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    public void setTransactionMode(TransactionMode transactionMode) {
        this.transactionMode = transactionMode;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getProviderSignature() {
        return providerSignature;
    }

    public void setProviderSignature(String providerSignature) {
        this.providerSignature = providerSignature;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getGatewayFee() {
        return gatewayFee;
    }

    public void setGatewayFee(BigDecimal gatewayFee) {
        this.gatewayFee = gatewayFee;
    }

    public String getPricingTier() {
        return pricingTier;
    }

    public void setPricingTier(String pricingTier) {
        this.pricingTier = pricingTier;
    }

    public long getVersion() {
        return version;
    }
}
