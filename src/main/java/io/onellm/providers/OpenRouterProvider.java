package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for OpenRouter - unified gateway to 100+ models.
 * Supports: All models available through OpenRouter.
 */
public class OpenRouterProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "openrouter/", "openai/", "anthropic/", "google/", "meta-llama/", 
            "mistralai/", "cohere/", "perplexity/", "deepseek/"
    );
    
    private final String siteName;
    private final String siteUrl;
    
    public OpenRouterProvider(String apiKey) {
        this(apiKey, null, null);
    }
    
    public OpenRouterProvider(String apiKey, String siteName, String siteUrl) {
        super(apiKey, DEFAULT_BASE_URL);
        this.siteName = siteName;
        this.siteUrl = siteUrl;
    }
    
    @Override
    public String getName() {
        return "openrouter";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    public boolean supportsModel(String modelName) {
        // OpenRouter supports many model formats
        if (modelName == null) return false;
        // If it contains a slash (org/model format), it's likely OpenRouter compatible
        return modelName.contains("/") || modelName.toLowerCase().startsWith("openrouter/");
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        if (siteName != null) {
            headers.put("X-Title", siteName);
        }
        if (siteUrl != null) {
            headers.put("HTTP-Referer", siteUrl);
        }
        return headers;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        return baseUrl + "/chat/completions";
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Remove openrouter/ prefix if present
        String model = request.getModel();
        if (model.toLowerCase().startsWith("openrouter/")) {
            model = model.substring(11);
        }
        
        body.put("model", model);
        body.put("messages", convertMessages(request.getMessages()));
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            body.put("frequency_penalty", request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            body.put("presence_penalty", request.getPresencePenalty());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop", request.getStop());
        }
        if (request.isStream()) {
            body.put("stream", true);
        }
        
        return body;
    }
    
    @Override
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new LLMException("openrouter", "No choices in response", 0);
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
                    usageObj.get("prompt_tokens").getAsInt(),
                    usageObj.get("completion_tokens").getAsInt(),
                    usageObj.get("total_tokens").getAsInt()
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
}
