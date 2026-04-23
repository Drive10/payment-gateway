package dev.payment.paymentservice.payment.exception;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalPaymentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalPaymentExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("event=api_exception code={} message={} path={}",
                ex.getErrorCode(), ex.getMessage(), request.getRequestURI());

        ErrorDetails error = new ErrorDetails(ex.getErrorCode(), ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.valueOf(ex.getHttpStatus())).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });

        log.warn("event=validation_failed path={} details={}", request.getRequestURI(), details);
        ErrorDetails error = new ErrorDetails("VALIDATION_ERROR", "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreakerException(CallNotPermittedException ex) {
        log.error("event=circuit_breaker_open message={}", ex.getMessage());
        ErrorDetails error = new ErrorDetails(
                "SERVICE_UNAVAILABLE",
                "Payment service is temporarily unavailable. Please retry later.",
                null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("event=authentication_failed message={}", ex.getMessage());
        ErrorDetails error = new ErrorDetails("AUTHENTICATION_FAILED", "Authentication required", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("event=access_denied message={}", ex.getMessage());
        ErrorDetails error = new ErrorDetails("ACCESS_DENIED", "You don't have permission to perform this action", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("event=unexpected_error message={}", ex.getMessage(), ex);
        ErrorDetails error = new ErrorDetails(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support if this persists.",
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }
}
