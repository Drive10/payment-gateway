package dev.payment.paymentservice.payment.domain.enums;

public enum LedgerAccountType {
    MERCHANT_WALLET,
    MERCHANT_RECEIVABLE,
    PLATFORM_FEE,
    PLATFORM_GATEWAY_FEE,
    CUSTOMER_ESCROW,
    BANK_SETTLEMENT,
    REFUND_RESERVE
}