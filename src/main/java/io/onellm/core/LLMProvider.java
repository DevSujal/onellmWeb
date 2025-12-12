package io.onellm.core;

import io.onellm.exception.LLMException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for LLM provider implementations.
 * Each provider (OpenAI, Anthropic, etc.) implements this interface.
 */
public interface LLMProvider {
    
    /**
     * Gets the provider name (e.g., "openai", "anthropic").
     */
    String getName();
    
    /**
     * Gets all model prefixes this provider handles.
     * Used for automatic routing based on model name.
     */
    List<String> getModelPrefixes();
    
    /**
     * Checks if this provider supports the given model.
     *
     * @param modelName The model name to check
     * @return true if this provider can handle the model
     */
    boolean supportsModel(String modelName);
    
    /**
     * Sends a completion request synchronously.
     *
     * @param request The LLM request
     * @return The LLM response
     * @throws LLMException If an error occurs
     */
    LLMResponse complete(LLMRequest request) throws LLMException;
    
    /**
     * Sends a completion request asynchronously.
     *
     * @param request The LLM request
     * @return A future containing the response
     */
    CompletableFuture<LLMResponse> completeAsync(LLMRequest request);
    
    /**
     * Sends a streaming completion request.
     *
     * @param request The LLM request
     * @param handler The stream handler for receiving chunks
     * @throws LLMException If an error occurs
     */
    void streamComplete(LLMRequest request, StreamHandler handler) throws LLMException;
    
    /**
     * Checks if this provider supports streaming.
     */
    default boolean supportsStreaming() {
        return true;
    }
}
