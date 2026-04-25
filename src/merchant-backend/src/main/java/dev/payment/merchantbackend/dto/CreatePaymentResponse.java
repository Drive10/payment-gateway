package dev.payment.merchantbackend.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentResponse {
    private boolean success;
    private Map<String, Object> data;
}
