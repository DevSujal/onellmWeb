package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for Groq's ultra-fast inference.
 * Supports: llama, mixtral, gemma models with fast inference.
 */
public class GroqProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
             "groq/"
    );
    
    public GroqProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public GroqProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "groq";
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
        
        // Remove groq/ prefix if present
        String model = request.getModel();
        if (model.toLowerCase().startsWith("groq/")) {
            model = model.substring(5);
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
            throw new LLMException("groq", "No choices in response", 0);
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
            // Llama models
            new ModelInfo("groq/llama-3.3-70b-versatile", "Llama 3.3 70B", "groq", "Latest Llama"),
            new ModelInfo("groq/llama-3.3-70b-specdec", "Llama 3.3 70B SpecDec", "groq", "Speculative decoding"),
            new ModelInfo("groq/llama-3.2-90b-vision-preview", "Llama 3.2 90B Vision", "groq", "Large vision"),
            new ModelInfo("groq/llama-3.2-11b-vision-preview", "Llama 3.2 11B Vision", "groq", "Vision model"),
            new ModelInfo("groq/llama-3.2-3b-preview", "Llama 3.2 3B", "groq", "Lightweight"),
            new ModelInfo("groq/llama-3.2-1b-preview", "Llama 3.2 1B", "groq", "Ultra-light"),
            new ModelInfo("groq/llama-3.1-70b-versatile", "Llama 3.1 70B", "groq", "Very capable"),
            new ModelInfo("groq/llama-3.1-8b-instant", "Llama 3.1 8B", "groq", "Fast"),
            new ModelInfo("groq/llama3-70b-8192", "Llama 3 70B", "groq", "Powerful"),
            new ModelInfo("groq/llama3-8b-8192", "Llama 3 8B", "groq", "Quick"),
            
            // Mixtral
            new ModelInfo("groq/mixtral-8x7b-32768", "Mixtral 8x7B", "groq", "MoE model"),
            
            // Gemma
            new ModelInfo("groq/gemma2-9b-it", "Gemma 2 9B", "groq", "Google model"),
            new ModelInfo("groq/gemma-7b-it", "Gemma 7B", "groq", "Google efficient"),
            
            // Whisper
            new ModelInfo("groq/whisper-large-v3", "Whisper Large V3", "groq", "Speech-to-text"),
            new ModelInfo("groq/whisper-large-v3-turbo", "Whisper Large V3 Turbo", "groq", "Fast transcription")
        );
    }
}
