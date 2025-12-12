package io.onellm.exception;

/**
 * Thrown when no provider is found for a given model.
 */
public class ModelNotFoundException extends LLMException {
    
    private final String model;
    
    public ModelNotFoundException(String model) {
        super("No provider found for model: " + model);
        this.model = model;
    }
    
    public String getModel() {
        return model;
    }
}
