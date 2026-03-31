package dev.payment.riskservice.exception;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RiskExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RiskExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(new ErrorDetails("BAD_REQUEST", ex.getMessage(), null)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(new ErrorDetails("INTERNAL_ERROR", "An unexpected error occurred", null)));
    }
}