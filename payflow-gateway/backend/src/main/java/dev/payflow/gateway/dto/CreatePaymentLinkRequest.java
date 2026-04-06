package dev.payflow.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreatePaymentLinkRequest {
    @NotBlank
    private String orderNumber;
    private String description;
    @NotNull @Positive
    private BigDecimal amount;
    @NotBlank
    private String currency;
    @NotBlank
    private String mode;

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
