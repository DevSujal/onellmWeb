package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for Ollama local models.
 * Supports: Any model available in local Ollama instance.
 */
public class OllamaProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "ollama/", "local/"
    );
    
    public OllamaProvider() {
        super("not-required", DEFAULT_BASE_URL);
    }
    
    public OllamaProvider(String baseUrl) {
        super("not-required", baseUrl);
    }
    
    @Override
    public String getName() {
        return "ollama";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        return baseUrl + "/api/chat";
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Remove ollama/ or local/ prefix if present
        String model = request.getModel();
        if (model.toLowerCase().startsWith("ollama/")) {
            model = model.substring(7);
        } else if (model.toLowerCase().startsWith("local/")) {
            model = model.substring(6);
        }
        
        body.put("model", model);
        body.put("messages", convertMessages(request.getMessages()));
        body.put("stream", request.isStream());
        
        // Ollama uses 'options' for generation parameters
        Map<String, Object> options = new HashMap<>();
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            options.put("num_predict", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            options.put("top_p", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            options.put("stop", request.getStop());
        }
        
        if (!options.isEmpty()) {
            body.put("options", options);
        }
        
        return body;
    }
    
    @Override
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        JsonObject message = response.getAsJsonObject("message");
        if (message == null) {
            throw new LLMException("ollama", "No message in response", 0);
        }
        
        String content = message.get("content").getAsString();
        
        Usage usage = null;
        if (response.has("prompt_eval_count") && response.has("eval_count")) {
            usage = new Usage(
                    response.get("prompt_eval_count").getAsInt(),
                    response.get("eval_count").getAsInt()
            );
        }
        
        return LLMResponse.builder()
                .model(response.has("model") ? response.get("model").getAsString() : model)
                .content(content)
                .finishReason(response.has("done") && response.get("done").getAsBoolean() ? "stop" : null)
                .usage(usage)
                .provider(getName())
                .latencyMs(latencyMs)
                .build();
    }
    
    @Override
    protected String extractContentFromStreamChunk(JsonObject chunk) {
        if (chunk.has("message")) {
            JsonObject message = chunk.getAsJsonObject("message");
            if (message.has("content")) {
                return message.get("content").getAsString();
            }
        }
        return null;
    }
    
    @Override
    protected String parseStreamChunk(String line) {
        // Ollama returns JSON directly, not SSE format
        try {
            JsonObject json = com.google.gson.JsonParser.parseString(line).getAsJsonObject();
            return extractContentFromStreamChunk(json);
        } catch (Exception e) {
            logger.trace("Failed to parse Ollama stream chunk: {}", line);
            return null;
        }
    }
}
