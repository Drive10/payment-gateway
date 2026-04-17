package dev.payment.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetails(
        String code,
        String message,
        Map<String, String> details
) {
}
