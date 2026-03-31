package dev.payment.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        BindingResult result = ex.getBindingResult();
        List<ApiError.FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .collect(Collectors.toList());

        ApiError error = ApiError.badRequest("Validation failed", request.getRequestURI())
                .withCode(ErrorCodes.VALIDATION_ERROR)
                .withCorrelationId(MDC.get("correlationId"))
                .withFieldErrors(fieldErrors);

        log.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        ApiError error = ApiError.badRequest(ex.getMessage(), request.getRequestURI())
                .withCode(ErrorCodes.INVALID_REQUEST)
                .withCorrelationId(MDC.get("correlationId"));

        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        ApiError error = ApiError.notFound(ex.getMessage(), request.getRequestURI())
                .withCode(ErrorCodes.RESOURCE_NOT_FOUND)
                .withCorrelationId(MDC.get("correlationId"));

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        ApiError error = ApiError.conflict(ex.getMessage(), request.getRequestURI())
                .withCode(ErrorCodes.DUPLICATE_RESOURCE)
                .withCorrelationId(MDC.get("correlationId"));

        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiError> handlePaymentException(
            PaymentException ex, HttpServletRequest request) {
        
        HttpStatus status = mapPaymentErrorToStatus(ex);
        ApiError error = new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage(), request.getRequestURI())
                .withCode(ex.getErrorCode())
                .withCorrelationId(MDC.get("correlationId"));

        log.error("Payment error: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> handleRateLimitException(
            RateLimitException ex, HttpServletRequest request) {
        
        ApiError error = ApiError.tooManyRequests(ex.getMessage(), request.getRequestURI())
                .withCode(ErrorCodes.RATE_LIMIT_EXCEEDED)
                .withCorrelationId(MDC.get("correlationId"));

        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        ApiError error = ApiError.internal("An unexpected error occurred", request.getRequestURI())
                .withCode(ErrorCodes.INTERNAL_ERROR)
                .withCorrelationId(MDC.get("correlationId"));

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private HttpStatus mapPaymentErrorToStatus(PaymentException ex) {
        return switch (ex.getErrorCode()) {
            case ErrorCodes.PAYMENT_DECLINED, ErrorCodes.CARD_DECLINED, 
                 ErrorCodes.INVALID_CARD, ErrorCodes.INSUFFICIENT_FUNDS -> HttpStatus.BAD_REQUEST;
            case ErrorCodes.PAYMENT_EXPIRED -> HttpStatus.GONE;
            case ErrorCodes.REFUND_EXCEEDED -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
