package dev.payment.simulatorservice.domain;

import dev.payment.simulatorservice.domain.enums.SimulationMode;
import dev.payment.simulatorservice.domain.enums.SimulationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "simulation_transactions")
public class SimulationTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_reference", nullable = false, length = 64)
    private String orderReference;

    @Column(name = "payment_reference", nullable = false, length = 64)
    private String paymentReference;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_order_id", nullable = false, unique = true, length = 80)
    private String providerOrderId;

    @Column(name = "provider_payment_id", unique = true, length = 80)
    private String providerPaymentId;

    @Column(name = "provider_signature", length = 255)
    private String providerSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulation_mode", nullable = false, length = 16)
    private SimulationMode simulationMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SimulationStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "checkout_url", nullable = false, length = 255)
    private String checkoutUrl;

    @Column(length = 255)
    private String notes;

    @Column(name = "webhook_callback_url", length = 512)
    private String webhookCallbackUrl;
}
