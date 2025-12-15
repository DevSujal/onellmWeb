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
 * Provider for Hugging Face Inference API.
 * Supports chat models available through Hugging Face's serverless inference or dedicated endpoints.
 * 
 * Authentication uses Bearer token with the user's Hugging Face API token (hf_token).
 */
public class HuggingFaceProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://router.huggingface.co";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "huggingface/", "hf/"
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

        // Hugging Face Router exposes an OpenAI-compatible endpoint:
        //   https://router.huggingface.co/v1/chat/completions
        String trimmedBaseUrl = baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        if (trimmedBaseUrl != null && trimmedBaseUrl.endsWith("/v1")) {
            return trimmedBaseUrl + "/chat/completions";
        }
        if (trimmedBaseUrl != null && trimmedBaseUrl.endsWith("/v1/chat/completions")) {
            return trimmedBaseUrl;
        }
        return trimmedBaseUrl + "/v1/chat/completions";
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

        // Remove provider prefix if present (preserve remaining slashes)
        String model = stripProviderPrefix(request.getModel());
        
        // Build messages in the format expected by HF chat completion API
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
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            // Meta Llama
            new ModelInfo("huggingface/meta-llama/Llama-3.3-70B-Instruct", "Llama 3.3 70B", "huggingface", "Meta latest"),
            new ModelInfo("huggingface/meta-llama/Llama-3.1-70B-Instruct", "Llama 3.1 70B", "huggingface", "Very capable"),
            new ModelInfo("huggingface/meta-llama/Llama-3.1-8B-Instruct", "Llama 3.1 8B", "huggingface", "Fast"),
            
            // Mistral
            new ModelInfo("huggingface/mistralai/Mistral-7B-Instruct-v0.3", "Mistral 7B v0.3", "huggingface", "Latest Mistral"),
            new ModelInfo("huggingface/mistralai/Mixtral-8x7B-Instruct-v0.1", "Mixtral 8x7B", "huggingface", "MoE model"),
            
            // Qwen
            new ModelInfo("huggingface/Qwen/Qwen2.5-72B-Instruct", "Qwen 2.5 72B", "huggingface", "Most capable"),
            new ModelInfo("huggingface/Qwen/Qwen2.5-32B-Instruct", "Qwen 2.5 32B", "huggingface", "Very capable"),
            new ModelInfo("huggingface/Qwen/Qwen2.5-7B-Instruct", "Qwen 2.5 7B", "huggingface", "Balanced"),
            new ModelInfo("huggingface/Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B", "huggingface", "Code specialist"),
            
            // Microsoft
            new ModelInfo("huggingface/microsoft/Phi-3-medium-4k-instruct", "Phi-3 Medium", "huggingface", "Microsoft balanced"),
            new ModelInfo("huggingface/microsoft/Phi-3-mini-4k-instruct", "Phi-3 Mini", "huggingface", "Microsoft lightweight"),
            
            // DeepSeek
            new ModelInfo("huggingface/deepseek-ai/DeepSeek-V3", "DeepSeek V3", "huggingface", "Latest DeepSeek"),
            new ModelInfo("huggingface/deepseek-ai/DeepSeek-R1", "DeepSeek R1", "huggingface", "Reasoning model"),
            
            // RWKV
            new ModelInfo("hf/rwkv7-g1a4-2.9b-20251118-ctx8192", "RWKV7 2.9B", "huggingface", "Standard chat", true),
            new ModelInfo("hf/rwkv7-g1a4-2.9b-20251118-ctx8192:thinking", "RWKV7 2.9B Thinking", "huggingface", "Chain-of-thought", true)
        );
    }
}
