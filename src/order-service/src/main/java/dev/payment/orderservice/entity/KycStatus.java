package dev.payment.orderservice.entity;

public enum KycStatus {
    PENDING("No documents submitted"),
    SUBMITTED("Documents submitted, pending review"),
    IN_REVIEW("Documents under review"),
    VERIFIED("KYC approved"),
    REJECTED("KYC rejected"),
    EXPIRED("KYC documents expired");

    private final String description;

    KycStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(KycStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == SUBMITTED;
            case SUBMITTED -> newStatus == IN_REVIEW || newStatus == REJECTED;
            case IN_REVIEW -> newStatus == VERIFIED || newStatus == REJECTED;
            case VERIFIED -> newStatus == EXPIRED;
            case REJECTED -> newStatus == SUBMITTED;
            case EXPIRED -> newStatus == SUBMITTED;
        };
    }

    public boolean isActive() {
        return this == VERIFIED;
    }

    public boolean canAcceptPayments() {
        return this == VERIFIED;
    }
}
