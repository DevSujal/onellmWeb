package io.onellm.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * Maps exceptions to appropriate HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle LLM-specific exceptions.
     */
    @ExceptionHandler(LLMException.class)
    public ResponseEntity<Map<String, Object>> handleLLMException(LLMException ex) {
        logger.error("LLM Exception: {}", ex.getMessage(), ex);
        
        HttpStatus status = determineHttpStatus(ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", Instant.now().toString());
        
        if (ex.getProvider() != null) {
            errorResponse.put("provider", ex.getProvider());
        }
        if (ex.getStatusCode() > 0) {
            errorResponse.put("statusCode", ex.getStatusCode());
        }
        if (ex.getErrorCode() != null) {
            errorResponse.put("errorCode", ex.getErrorCode());
        }
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle model not found exceptions.
     */
    @ExceptionHandler(ModelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleModelNotFoundException(ModelNotFoundException ex) {
        logger.warn("Model not found: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("type", "model_not_found");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle provider not configured exceptions.
     */
    @ExceptionHandler(ProviderNotConfiguredException.class)
    public ResponseEntity<Map<String, Object>> handleProviderNotConfiguredException(ProviderNotConfiguredException ex) {
        logger.warn("Provider not configured: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("type", "provider_not_configured");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", "Validation failed");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("type", "validation_error");
        errorResponse.put("fields", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error", ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("type", "internal_error");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Determines the HTTP status based on the LLM exception.
     */
    private HttpStatus determineHttpStatus(LLMException ex) {
        if (ex.isAuthenticationError()) {
            return HttpStatus.UNAUTHORIZED;
        } else if (ex.isRateLimitError()) {
            return HttpStatus.TOO_MANY_REQUESTS;
        } else if (ex.isServerError()) {
            return HttpStatus.BAD_GATEWAY;
        } else if (ex.isClientError()) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
