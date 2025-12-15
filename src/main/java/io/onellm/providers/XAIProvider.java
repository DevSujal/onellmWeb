package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for xAI's Grok models.
 * Supports: grok-1, grok-2, grok-beta, etc.
 */
public class XAIProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://api.x.ai/v1";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "grok/", "xai/"
    );
    
    public XAIProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public XAIProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "xai";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        return baseUrl + "/chat/completions";
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Remove xai/ prefix if present
        String model = request.getModel();
        if (model.toLowerCase().startsWith("xai/")) {
            model = model.substring(4);
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
            throw new LLMException("xai", "No choices in response", 0);
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
    
    @Override
    protected String getModelsEndpoint() {
        return baseUrl + "/models";
    }
    
    @Override
    public List<ModelInfo> getAvailableModels() {
        return fetchDynamicModels();
    }
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            new ModelInfo("xai/grok-2-latest", "Grok 2", "xai", "Latest Grok"),
            new ModelInfo("xai/grok-2-1212", "Grok 2 1212", "xai", "131K context"),
            new ModelInfo("xai/grok-2-vision-1212", "Grok 2 Vision", "xai", "Multimodal"),
            new ModelInfo("xai/grok-beta", "Grok Beta", "xai", "Experimental"),
            new ModelInfo("xai/grok-vision-beta", "Grok Vision Beta", "xai", "Vision experimental")
        );
    }
}
