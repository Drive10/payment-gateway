package dev.payment.simulatorservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfo {
    private String bin;
    private String lastFour;
    private String brand;
    private String type;
    private String cardHolderName;
    private boolean requires3ds;
    private String bankName;
    private String country;
    private String cardLevel;
}
