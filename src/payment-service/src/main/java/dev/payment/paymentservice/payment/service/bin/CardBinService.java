package dev.payment.paymentservice.payment.service.bin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CardBinService {

    private static final Map<String, CardBinData> BIN_CACHE = new ConcurrentHashMap<>();

    public CardBinData getCardData(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 6) {
            return getDefaultCardData();
        }

        String bin = cardNumber.substring(0, 6);

        return BIN_CACHE.computeIfAbsent(bin, this::lookupBin);
    }

    private CardBinData lookupBin(String bin) {
        log.info("Looking up BIN: {}", bin);

        CardBinData data = MOCK_BIN_DATABASE.get(bin);
        if (data != null) {
            log.info("BIN {} found: {} {}", bin, data.brand(), data.type());
            return data;
        }

        log.info("BIN {} not found in database, using default", bin);
        return getDefaultCardData();
    }

    private CardBinData getDefaultCardData() {
        return CardBinData.builder()
                .brand("UNKNOWN")
                .type("CREDIT")
                .category("STANDARD")
                .country("US")
                .countryName("United States")
                .bankName("Unknown Bank")
                .is3dsSupported(true)
                .isInternational(true)
                .build();
    }

    public record CardBinData(
            String brand,
            String type,
            String category,
            String country,
            String countryName,
            String bankName,
            String bankCode,
            boolean is3dsSupported,
            boolean isInternational,
            String cardLevel
    ) {
        public static CardBinDataBuilder builder() {
            return new CardBinDataBuilder();
        }
    }

    public static class CardBinDataBuilder {
        private String brand = "UNKNOWN";
        private String type = "CREDIT";
        private String category = "STANDARD";
        private String country = "US";
        private String countryName = "United States";
        private String bankName = "Unknown Bank";
        private String bankCode = null;
        private boolean is3dsSupported = true;
        private boolean isInternational = true;
        private String cardLevel = "CLASSIC";

        public CardBinDataBuilder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public CardBinDataBuilder type(String type) {
            this.type = type;
            return this;
        }

        public CardBinDataBuilder category(String category) {
            this.category = category;
            return this;
        }

        public CardBinDataBuilder country(String country) {
            this.country = country;
            return this;
        }

        public CardBinDataBuilder countryName(String countryName) {
            this.countryName = countryName;
            return this;
        }

        public CardBinDataBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public CardBinDataBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }

        public CardBinDataBuilder is3dsSupported(boolean is3dsSupported) {
            this.is3dsSupported = is3dsSupported;
            return this;
        }

        public CardBinDataBuilder isInternational(boolean isInternational) {
            this.isInternational = isInternational;
            return this;
        }

        public CardBinDataBuilder cardLevel(String cardLevel) {
            this.cardLevel = cardLevel;
            return this;
        }

        public CardBinData build() {
            return new CardBinData(brand, type, category, country, countryName, bankName, bankCode, is3dsSupported, isInternational, cardLevel);
        }
    }

    private static final Map<String, CardBinData> MOCK_BIN_DATABASE = Map.ofEntries(
            Map.entry("411111", CardBinData.builder()
                    .brand("VISA")
                    .type("DEBIT")
                    .category("CLASSIC")
                    .country("US")
                    .countryName("United States")
                    .bankName("Bank of America")
                    .bankCode("BOA")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("CLASSIC")
                    .build()),
            Map.entry("400000", CardBinData.builder()
                    .brand("VISA")
                    .type("CREDIT")
                    .category("PLATINUM")
                    .country("US")
                    .countryName("United States")
                    .bankName("Chase Bank")
                    .bankCode("CHASE")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("PLATINUM")
                    .build()),
            Map.entry("555555", CardBinData.builder()
                    .brand("MASTERCARD")
                    .type("DEBIT")
                    .category("STANDARD")
                    .country("US")
                    .countryName("United States")
                    .bankName("Wells Fargo")
                    .bankCode("WF")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("STANDARD")
                    .build()),
            Map.entry("520000", CardBinData.builder()
                    .brand("MASTERCARD")
                    .type("CREDIT")
                    .category("GOLD")
                    .country("IN")
                    .countryName("India")
                    .bankName("HDFC Bank")
                    .bankCode("HDFC")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("GOLD")
                    .build()),
            Map.entry("378282", CardBinData.builder()
                    .brand("AMEX")
                    .type("CREDIT")
                    .category("PLATINUM")
                    .country("US")
                    .countryName("United States")
                    .bankName("American Express")
                    .bankCode("AMEX")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("PLATINUM")
                    .build()),
            Map.entry("601100", CardBinData.builder()
                    .brand("DISCOVER")
                    .type("CREDIT")
                    .category("STANDARD")
                    .country("US")
                    .countryName("United States")
                    .bankName("Discover Bank")
                    .bankCode("DISC")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("STANDARD")
                    .build()),
            Map.entry("506199", CardBinData.builder()
                    .brand("MAESTRO")
                    .type("DEBIT")
                    .category("STANDARD")
                    .country("GB")
                    .countryName("United Kingdom")
                    .bankName("Barclays")
                    .bankCode("BARCLAYS")
                    .is3dsSupported(true)
                    .isInternational(true)
                    .cardLevel("STANDARD")
                    .build()),
            Map.entry("587532", CardBinData.builder()
                    .brand("RUPAY")
                    .type("DEBIT")
                    .category("PLATINUM")
                    .country("IN")
                    .countryName("India")
                    .bankName("State Bank of India")
                    .bankCode("SBI")
                    .is3dsSupported(false)
                    .isInternational(false)
                    .cardLevel("PLATINUM")
                    .build())
    );
}