package dev.payment.common.api;

public record Meta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
