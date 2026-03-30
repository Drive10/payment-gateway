package dev.payment.ledgerservice.exception;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.failure(new ErrorDetails(exception.getCode(), exception.getMessage(), null)));
    }
}
