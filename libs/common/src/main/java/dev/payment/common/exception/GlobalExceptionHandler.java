package dev.payment.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(PayFlowException.class)
    public ResponseEntity<Object> handlePayFlowException(PayFlowException ex, WebRequest request) {
        logger.warn("PayFlow exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorDetails errorDetails = new ErrorDetails(
                ex.getErrorCode(),
                ex.getMessage(),
                new HashMap<>());

        ApiResponse<Object> response = ApiResponse.failure(errorDetails);
        return new ResponseEntity<>(response, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception", ex);

        ErrorDetails errorDetails = new ErrorDetails(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                new HashMap<>());

        ApiResponse<Object> response = ApiResponse.failure(errorDetails);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> createErrorBody(String errorCode, String message, Throwable ex) {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("error", Map.of(
                "code", errorCode,
                "message", message
        ));
        if (ex != null) {
            errorBody.put("exception", ex.getClass().getName());
            errorBody.put("details", ex.getMessage());
        }
        return errorBody;
    }
}
