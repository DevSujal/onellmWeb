package io.onellm.exception;

/**
 * Base exception for all LLM-related errors.
 */
public class LLMException extends RuntimeException {
    
    private final String provider;
    private final int statusCode;
    private final String errorCode;
    
    public LLMException(String message) {
        super(message);
        this.provider = null;
        this.statusCode = 0;
        this.errorCode = null;
    }
    
    public LLMException(String message, Throwable cause) {
        super(message, cause);
        this.provider = null;
        this.statusCode = 0;
        this.errorCode = null;
    }
    
    public LLMException(String provider, String message, int statusCode) {
        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
        this.errorCode = null;
    }
    
    public LLMException(String provider, String message, int statusCode, String errorCode) {
        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public LLMException(String provider, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.statusCode = statusCode;
        this.errorCode = null;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public boolean isRateLimitError() {
        return statusCode == 429;
    }
    
    public boolean isAuthenticationError() {
        return statusCode == 401 || statusCode == 403;
    }
    
    public boolean isServerError() {
        return statusCode >= 500;
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
}
