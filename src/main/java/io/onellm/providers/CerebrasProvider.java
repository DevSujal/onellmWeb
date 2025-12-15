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
 * Provider for Cerebras fast inference.
 * Supports: llama models with extremely fast inference.
 */
public class CerebrasProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://api.cerebras.ai/v1";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "cerebras/"
    );
    
    public CerebrasProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public CerebrasProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "cerebras";
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
        
        // Remove cerebras/ prefix if present
        String model = request.getModel();
        if (model.toLowerCase().startsWith("cerebras/")) {
            model = model.substring(9);
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
            throw new LLMException("cerebras", "No choices in response", 0);
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
            new ModelInfo("cerebras/llama3.1-8b", "Llama 3.1 8B", "cerebras", "Fast inference"),
            new ModelInfo("cerebras/llama3.1-70b", "Llama 3.1 70B", "cerebras", "Very capable"),
            new ModelInfo("cerebras/llama-3.3-70b", "Llama 3.3 70B", "cerebras", "Latest Llama"),
            new ModelInfo("cerebras/qwen3-32b", "Qwen3 32B", "cerebras", "Alibaba model"),
            new ModelInfo("cerebras/qwen-coder", "Qwen Coder", "cerebras", "Code specialist")
        );
    }
}
