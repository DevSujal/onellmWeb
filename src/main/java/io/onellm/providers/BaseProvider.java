package io.onellm.providers;

import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.exception.LLMException;
import io.onellm.util.HttpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for all LLM providers with common functionality.
 */
public abstract class BaseProvider implements LLMProvider {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final String apiKey;
    protected final HttpClientWrapper httpClient;
    protected final String baseUrl;
    
    protected BaseProvider(String apiKey, String baseUrl) {
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.httpClient = new HttpClientWrapper();
    }
    
    protected BaseProvider(String apiKey, String baseUrl, HttpClientWrapper httpClient) {
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.httpClient = Objects.requireNonNull(httpClient, "HTTP client cannot be null");
    }
    
    @Override
    public boolean supportsModel(String modelName) {
        if (modelName == null) return false;
        String lowerModel = modelName.toLowerCase();
        return getModelPrefixes().stream()
                .anyMatch(prefix -> lowerModel.startsWith(prefix.toLowerCase()));
    }
    
    @Override
    public CompletableFuture<LLMResponse> completeAsync(LLMRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request));
    }
    
    /**
     * Gets authorization headers for the provider.
     */
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    /**
     * Builds the request body for this provider.
     */
    protected abstract Map<String, Object> buildRequestBody(LLMRequest request);
    
    /**
     * Parses the response from this provider.
     */
    protected abstract LLMResponse parseResponse(JsonObject response, String model, long latencyMs);
    
    /**
     * Gets the API endpoint for completions.
     */
    protected abstract String getCompletionEndpoint();
    
    @Override
    public LLMResponse complete(LLMRequest request) throws LLMException {
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> body = buildRequestBody(request);
        String endpoint = getCompletionEndpoint();
        
        logger.debug("Sending request to {} for model {}", endpoint, request.getModel());
        
        JsonObject response = httpClient.post(endpoint, getHeaders(), body);
        
        long latencyMs = System.currentTimeMillis() - startTime;
        return parseResponse(response, request.getModel(), latencyMs);
    }
    
    @Override
    public void streamComplete(LLMRequest request, StreamHandler handler) throws LLMException {
        Map<String, Object> body = buildRequestBody(request);
        body.put("stream", true);
        
        String endpoint = getCompletionEndpoint();
        StringBuilder fullContent = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        logger.debug("Starting stream to endpoint: {}", endpoint);
        
        httpClient.postStream(endpoint, getHeaders(), body,
            line -> {
                logger.debug("BaseProvider received line: [{}]", line.length() > 100 ? line.substring(0, 100) + "..." : line);
                String chunk = parseStreamChunk(line);
                logger.debug("Parsed chunk: [{}]", chunk);
                if (chunk != null && !chunk.isEmpty()) {
                    fullContent.append(chunk);
                    handler.onChunk(chunk);
                    logger.debug("Accumulated content length: {}", fullContent.length());
                }
            },
            error -> handler.onError(error)
        );
        
        long latencyMs = System.currentTimeMillis() - startTime;
        LLMResponse response = LLMResponse.builder()
                .content(fullContent.toString())
                .model(request.getModel())
                .provider(getName())
                .latencyMs(latencyMs)
                .build();
        handler.onComplete(response);
    }
    
    /**
     * Parses a streaming chunk. Override in subclasses for provider-specific parsing.
     */
    protected String parseStreamChunk(String line) {
        if (line.startsWith("data: ")) {
            String data = line.substring(6);
            if ("[DONE]".equals(data)) {
                return null;
            }
            try {
                JsonObject json = com.google.gson.JsonParser.parseString(data).getAsJsonObject();
                return extractContentFromStreamChunk(json);
            } catch (Exception e) {
                logger.trace("Failed to parse stream chunk: {}", line);
                return null;
            }
        }
        return null;
    }
    
    /**
     * Extracts content from a parsed stream chunk JSON.
     */
    protected abstract String extractContentFromStreamChunk(JsonObject chunk);
    
    /**
     * Converts messages to the format expected by this provider.
     */
    protected List<Map<String, String>> convertMessages(List<Message> messages) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Message msg : messages) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            result.add(m);
        }
        return result;
    }
}
