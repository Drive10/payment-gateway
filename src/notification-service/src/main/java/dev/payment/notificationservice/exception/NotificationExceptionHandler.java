package dev.payment.notificationservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class NotificationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "BAD_REQUEST", "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "INTERNAL_ERROR", "message", "An unexpected error occurred"));
    }
}