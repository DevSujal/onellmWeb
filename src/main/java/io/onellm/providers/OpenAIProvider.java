package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for OpenAI's GPT models.
 * Supports: gpt-4, gpt-4-turbo, gpt-4o, gpt-3.5-turbo, etc.
 */
public class OpenAIProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "gpt/", "openai/"
    );
    
    public OpenAIProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public OpenAIProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "openai";
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
        body.put("model", request.getModel());
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
            throw new LLMException("openai", "No choices in response", 0);
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
        // Dynamically fetch models from OpenAI
        return fetchDynamicModels();
    }
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            // GPT-4o family
            new ModelInfo("openai/gpt-4o", "GPT-4o", "openai", "Flagship multimodal"),
            new ModelInfo("openai/gpt-4o-2024-11-20", "GPT-4o (Nov 2024)", "openai", "Latest GPT-4o"),
            new ModelInfo("openai/gpt-4o-mini", "GPT-4o Mini", "openai", "Fast & affordable"),
            new ModelInfo("openai/chatgpt-4o-latest", "ChatGPT-4o Latest", "openai", "ChatGPT version"),
            
            // GPT-4 family
            new ModelInfo("openai/gpt-4-turbo", "GPT-4 Turbo", "openai", "Latest GPT-4"),
            new ModelInfo("openai/gpt-4-turbo-preview", "GPT-4 Turbo Preview", "openai", "Preview version"),
            new ModelInfo("openai/gpt-4", "GPT-4", "openai", "Original GPT-4"),
            new ModelInfo("openai/gpt-4-vision-preview", "GPT-4 Vision", "openai", "Vision capable"),
            
            // GPT-3.5 family
            new ModelInfo("openai/gpt-3.5-turbo", "GPT-3.5 Turbo", "openai", "Fast & cheap"),
            new ModelInfo("openai/gpt-3.5-turbo-0125", "GPT-3.5 Turbo 0125", "openai", "Latest 3.5"),
            new ModelInfo("openai/gpt-3.5-turbo-16k", "GPT-3.5 Turbo 16K", "openai", "Extended context"),
            
            // O-series reasoning models
            new ModelInfo("openai/o1", "O1", "openai", "Advanced reasoning"),
            new ModelInfo("openai/o1-preview", "O1 Preview", "openai", "Reasoning preview"),
            new ModelInfo("openai/o1-mini", "O1 Mini", "openai", "Fast reasoning"),
            new ModelInfo("openai/o3-mini", "O3 Mini", "openai", "Latest reasoning")
        );
    }
}
