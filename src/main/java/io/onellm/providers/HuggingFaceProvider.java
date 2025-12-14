package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for Hugging Face Inference API.
 * Supports chat models available through Hugging Face's serverless inference or dedicated endpoints.
 * 
 * Authentication uses Bearer token with the user's Hugging Face API token (hf_token).
 */
public class HuggingFaceProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://api-inference.huggingface.co/models";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "huggingface/", "hf/", "meta-llama/", "mistralai/", "microsoft/", 
            "google/flan", "tiiuae/", "bigscience/", "HuggingFaceH4/", "Qwen/"
    );
    
    private final String modelEndpoint;
    
    public HuggingFaceProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
        this.modelEndpoint = null;
    }
    
    public HuggingFaceProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
        this.modelEndpoint = null;
    }
    
    /**
     * Constructor for dedicated inference endpoints.
     */
    public HuggingFaceProvider(String apiKey, String baseUrl, String modelEndpoint) {
        super(apiKey, baseUrl);
        this.modelEndpoint = modelEndpoint;
    }
    
    @Override
    public String getName() {
        return "huggingface";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        // For dedicated endpoints, use the modelEndpoint directly
        if (modelEndpoint != null && !modelEndpoint.isEmpty()) {
            return modelEndpoint;
        }
        // For serverless API, the model is appended to the base URL
        return baseUrl;
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Remove provider prefix if present
        String model = request.getModel();
        if (model.toLowerCase().startsWith("huggingface/")) {
            model = model.substring(12);
        } else if (model.toLowerCase().startsWith("hf/")) {
            model = model.substring(3);
        }
        
        // Build messages in the format expected by HF chat completion API
        List<Map<String, Object>> messages = convertMessages(request.getMessages());
        body.put("messages", messages);
        body.put("model", model);
        
        // Parameters wrapper for HF API
        Map<String, Object> parameters = new HashMap<>();
        if (request.getTemperature() != null) {
            parameters.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            parameters.put("max_new_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            parameters.put("top_p", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            parameters.put("stop", request.getStop());
        }
        if (!parameters.isEmpty()) {
            body.put("parameters", parameters);
        }
        
        if (request.isStream()) {
            body.put("stream", true);
        }
        
        return body;
    }
    
    /**
     * Builds the full endpoint URL including the model name for serverless API.
     */
    public String getEndpointForModel(String model) {
        if (modelEndpoint != null && !modelEndpoint.isEmpty()) {
            return modelEndpoint;
        }
        
        // Remove provider prefix if present
        if (model.toLowerCase().startsWith("huggingface/")) {
            model = model.substring(12);
        } else if (model.toLowerCase().startsWith("hf/")) {
            model = model.substring(3);
        }
        
        return baseUrl + "/" + model + "/v1/chat/completions";
    }
    
    @Override
    public LLMResponse complete(LLMRequest request) throws LLMException {
        long startTime = System.currentTimeMillis();
        
        try {
            String endpoint = getEndpointForModel(request.getModel());
            Map<String, Object> requestBody = buildRequestBody(request);
            
            String responseBody = httpClient.post(endpoint, requestBody, getHeaders());
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            
            long latencyMs = System.currentTimeMillis() - startTime;
            return parseResponse(response, request.getModel(), latencyMs);
            
        } catch (Exception e) {
            throw new LLMException(getName(), e.getMessage(), 0);
        }
    }
    
    @Override
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        // HF API returns OpenAI-compatible format for chat completions
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            // Try alternative format (text generation)
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
        // Alternative streaming format
        if (chunk.has("token") && chunk.getAsJsonObject("token").has("text")) {
            return chunk.getAsJsonObject("token").get("text").getAsString();
        }
        return null;
    }
}
