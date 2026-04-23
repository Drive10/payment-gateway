package dev.payment.combinedservice.payment.dto.response;

import dev.payment.combinedservice.payment.service.bin.CardBinService.CardBinData;

public record CardTokenizationResponse(
    String token,
    String lastFour,
    String cardBrand,
    String cardType,
    String cardCategory,
    String bankName,
    String country,
    String countryName,
    boolean requires3ds,
    boolean isInternational
) {
    public CardTokenizationResponse(String token, CardBinData binData) {
        this(
            token,
            token.length() >= 4 ? token.substring(token.length() - 4) : "****",
            binData.brand(),
            binData.type(),
            binData.category(),
            binData.bankName(),
            binData.country(),
            binData.countryName(),
            binData.is3dsSupported(),
            binData.isInternational()
        );
    }
}