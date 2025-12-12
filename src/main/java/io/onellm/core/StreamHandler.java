package io.onellm.core;

/**
 * Handler for streaming LLM responses.
 */
public interface StreamHandler {
    
    /**
     * Called when a new chunk of content is received.
     *
     * @param chunk The content chunk
     */
    void onChunk(String chunk);
    
    /**
     * Called when the stream is complete.
     *
     * @param response The complete response
     */
    void onComplete(LLMResponse response);
    
    /**
     * Called when an error occurs during streaming.
     *
     * @param error The error
     */
    void onError(Throwable error);
}
