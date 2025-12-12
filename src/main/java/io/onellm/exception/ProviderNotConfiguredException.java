package io.onellm.exception;

/**
 * Thrown when a provider is not configured but a model requires it.
 */
public class ProviderNotConfiguredException extends LLMException {
    
    private final String providerName;
    
    public ProviderNotConfiguredException(String providerName) {
        super("Provider not configured: " + providerName + ". Please add an API key.");
        this.providerName = providerName;
    }
    
    public String getProviderName() {
        return providerName;
    }
}
