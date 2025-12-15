package io.onellm.providers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.onellm.core.LLMRequest;
import io.onellm.core.LLMResponse;
import io.onellm.core.Usage;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

/**
 * Provider for FreeLLM - A free LLM inference API hosted on Hugging Face Spaces.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>No API key required - completely free to use</li>
 *   <li>No rate limiting or billing</li>
 *   <li>OpenAI-compatible endpoints</li>
 *   <li>Supports various CPU-friendly models including TinyLlama, Qwen, and microsoft/phi-2</li>
 * </ul>
 * 
 * <p>Recommended models:</p>
 * <ul>
 *   <li>TinyLlama/TinyLlama-1.1B-Chat-v1.0 - Fast, 1.1B params</li>
 *   <li>Qwen/Qwen2.5-0.5B-Instruct - Very fast, 0.5B params</li>
 *   <li>Qwen/Qwen2.5-1.5B-Instruct - Balanced, 1.5B params</li>
 *   <li>microsoft/phi-2 - High quality, 2.7B params</li>
 * </ul>
 * 
 * @see <a href="https://huggingface.co/spaces/mabemi/freellm">FreeLLM on Hugging Face</a>
 */
public class FreeLLMProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://mabemi-freellm.hf.space";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "freellm/", "free/"
    );
    
    /**
     * Creates a FreeLLM provider with default settings.
     * No API key is required as this is a free service.
     */
    public FreeLLMProvider() {
        super("", DEFAULT_BASE_URL); // Empty API key - not required
    }
    
    /**
     * Creates a FreeLLM provider with a custom base URL.
     * Useful if you're hosting your own FreeLLM instance.
     * 
     * @param baseUrl Custom base URL for the FreeLLM service
     */
    public FreeLLMProvider(String baseUrl) {
        super("", baseUrl);
    }
    
    @Override
    public String getName() {
        return "freellm";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        String trimmedBaseUrl = baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        
        return trimmedBaseUrl + "/v1/chat/completions";
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        // No Authorization header needed - FreeLLM is free to use
        return headers;
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();

        // Remove provider prefix if present
        String model = stripProviderPrefix(request.getModel());
        
        // Default to TinyLlama if no model specified
        if (model == null || model.isEmpty()) {
            model = "TinyLlama/TinyLlama-1.1B-Chat-v1.0";
        }
        
        // Build messages in OpenAI-compatible format
        List<Map<String, String>> messages = convertMessages(request.getMessages());
        body.put("messages", messages);
        body.put("model", model);

        // OpenAI-compatible parameters
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        
        if (request.isStream()) {
            body.put("stream", true);
        }
        
        return body;
    }

    private String stripProviderPrefix(String model) {
        if (model == null) {
            return null;
        }
        String lower = model.toLowerCase();
        for (String prefix : MODEL_PREFIXES) {
            if (prefix != null && !prefix.isEmpty() && lower.startsWith(prefix.toLowerCase())) {
                return model.substring(prefix.length());
            }
        }
        return model;
    }
    
    @Override
    public LLMResponse complete(LLMRequest request) throws LLMException {
        long startTime = System.currentTimeMillis();
        
        try {
            String endpoint = getCompletionEndpoint();
            Map<String, Object> requestBody = buildRequestBody(request);
            
            JsonObject response = httpClient.post(endpoint, getHeaders(), requestBody);
            
            long latencyMs = System.currentTimeMillis() - startTime;
            return parseResponse(response, request.getModel(), latencyMs);
            
        } catch (Exception e) {
            throw new LLMException(getName(), e.getMessage(), 0);
        }
    }
    
    @Override
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        // FreeLLM API returns OpenAI-compatible format
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            // Try alternative format (simple generation)
            if (response.has("generated_text")) {
                String content = response.get("generated_text").getAsString();
                return LLMResponse.builder()
                        .model(model)
                        .content(content)
                        .provider(getName())
                        .latencyMs(latencyMs)
                        .build();
            }
            throw new LLMException(getName(), "No choices in response", 0);
        }
        
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.getAsJsonObject("message");
        String content = message.get("content").getAsString();
        String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()
                ? choice.get("finish_reason").getAsString() : null;
        
        Usage usage = null;
        if (response.has("usage")) {
            JsonObject usageObj = response.getAsJsonObject("usage");
            usage = new Usage(
                    usageObj.has("prompt_tokens") ? usageObj.get("prompt_tokens").getAsInt() : 0,
                    usageObj.has("completion_tokens") ? usageObj.get("completion_tokens").getAsInt() : 0,
                    usageObj.has("total_tokens") ? usageObj.get("total_tokens").getAsInt() : 0
            );
        }
        
        return LLMResponse.builder()
                .id(response.has("id") ? response.get("id").getAsString() : null)
                .model(response.has("model") ? response.get("model").getAsString() : model)
                .content(content)
                .finishReason(finishReason)
                .usage(usage)
                .provider(getName())
                .latencyMs(latencyMs)
                .build();
    }
    
    @Override
    protected String extractContentFromStreamChunk(JsonObject chunk) {
        JsonArray choices = chunk.getAsJsonArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JsonObject choice = choices.get(0).getAsJsonObject();
            if (choice.has("delta")) {
                JsonObject delta = choice.getAsJsonObject("delta");
                if (delta.has("content") && !delta.get("content").isJsonNull()) {
                    return delta.get("content").getAsString();
                }
            }
        }
        return null;
    }
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            new ModelInfo("freellm/TinyLlama/TinyLlama-1.1B-Chat-v1.0", "TinyLlama 1.1B", "freellm", "Fast, lightweight", true),
            new ModelInfo("freellm/Qwen/Qwen2.5-0.5B-Instruct", "Qwen 0.5B", "freellm", "Ultra-fast", true),
            new ModelInfo("freellm/Qwen/Qwen2.5-1.5B-Instruct", "Qwen 1.5B", "freellm", "Balanced", true),
            new ModelInfo("freellm/microsoft/phi-2", "Phi-2", "freellm", "High quality, 2.7B", true)
        );
    }
}
