package dev.payment.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String correlationId;
    private String code;
    private List<FieldError> fieldErrors;
    private Map<String, Object> metadata;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;

        public FieldError() {}

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getRejectedValue() { return rejectedValue; }
        public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
    }

    public static ApiError badRequest(String message, String path) {
        return new ApiError(400, "Bad Request", message, path);
    }

    public static ApiError unauthorized(String message, String path) {
        return new ApiError(401, "Unauthorized", message, path);
    }

    public static ApiError forbidden(String message, String path) {
        return new ApiError(403, "Forbidden", message, path);
    }

    public static ApiError notFound(String message, String path) {
        return new ApiError(404, "Not Found", message, path);
    }

    public static ApiError conflict(String message, String path) {
        return new ApiError(409, "Conflict", message, path);
    }

    public static ApiError tooManyRequests(String message, String path) {
        return new ApiError(429, "Too Many Requests", message, path);
    }

    public static ApiError internal(String message, String path) {
        return new ApiError(500, "Internal Server Error", message, path);
    }

    public static ApiError serviceUnavailable(String message, String path) {
        return new ApiError(503, "Service Unavailable", message, path);
    }

    public ApiError withCode(String code) {
        this.code = code;
        return this;
    }

    public ApiError withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    public ApiError withFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
        return this;
    }

    public ApiError withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
