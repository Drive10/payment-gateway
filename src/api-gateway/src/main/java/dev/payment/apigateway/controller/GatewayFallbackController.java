package dev.payment.apigateway.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayFallbackController {

    @GetMapping("/fallback/payment")
    public ResponseEntity<ApiResponse<Void>> paymentFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(new ErrorDetails(
                        "PAYMENT_SERVICE_UNAVAILABLE",
                        "Payment service is temporarily unavailable",
                        null
                )));
    }

    @GetMapping("/fallback/platform")
    public ResponseEntity<ApiResponse<Void>> platformFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(new ErrorDetails(
                        "DOWNSTREAM_SERVICE_UNAVAILABLE",
                        "Downstream platform service is temporarily unavailable",
                        null
                )));
    }
}
