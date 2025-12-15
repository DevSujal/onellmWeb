package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for Ollama local models.
 * Supports: Any model available in local Ollama instance.
 */
public class OllamaProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://mabemi-my-ollama.hf.space";
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
    
    @Override
    protected String getModelsEndpoint() {
        return baseUrl + "/api/tags";
    }
    
    @Override
    public List<ModelInfo> getAvailableModels() {
        // Try to fetch from Ollama's /api/tags endpoint
        try {
            JsonObject response = httpClient.get(getModelsEndpoint(), getHeaders());
            List<ModelInfo> models = new ArrayList<>();
            
            if (response.has("models") && response.get("models").isJsonArray()) {
                for (var element : response.getAsJsonArray("models")) {
                    JsonObject modelObj = element.getAsJsonObject();
                    String name = modelObj.has("name") ? modelObj.get("name").getAsString() : null;
                    if (name != null) {
                        ModelInfo model = ModelInfo.builder()
                                .id("ollama/" + name)
                                .name(name)
                                .provider(getName())
                                .description("Ollama local model")
                                .free(true)
                                .build();
                        models.add(model);
                    }
                }
            }
            return models.isEmpty() ? getStaticModels() : models;
        } catch (Exception e) {
            logger.warn("Failed to fetch Ollama models, using static list: {}", e.getMessage());
            return getStaticModels();
        }
    }
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            // Gemma (Google)
            new ModelInfo("ollama/gemma3:1b", "Gemma3 1B", "ollama", "Google ultra-light", true),
            new ModelInfo("ollama/gemma3:4b", "Gemma3 4B", "ollama", "Google balanced", true),
            new ModelInfo("ollama/gemma2:2b", "Gemma2 2B", "ollama", "Google efficient", true),
            new ModelInfo("ollama/gemma2:9b", "Gemma2 9B", "ollama", "Google capable", true),
            
            // Llama (Meta)
            new ModelInfo("ollama/llama3.3:70b", "Llama 3.3 70B", "ollama", "Latest Meta flagship", true),
            new ModelInfo("ollama/llama3.2:1b", "Llama 3.2 1B", "ollama", "Ultra lightweight", true),
            new ModelInfo("ollama/llama3.2:3b", "Llama 3.2 3B", "ollama", "Lightweight", true),
            new ModelInfo("ollama/llama3.1:8b", "Llama 3.1 8B", "ollama", "Fast & capable", true),
            new ModelInfo("ollama/llama3.1:70b", "Llama 3.1 70B", "ollama", "Very capable", true),
            
            // Mistral
            new ModelInfo("ollama/mistral:7b", "Mistral 7B", "ollama", "Powerful open model", true),
            new ModelInfo("ollama/mixtral:8x7b", "Mixtral 8x7B", "ollama", "MoE model", true),
            
            // Qwen (Alibaba)
            new ModelInfo("ollama/qwen2.5:0.5b", "Qwen 2.5 0.5B", "ollama", "Ultra-fast", true),
            new ModelInfo("ollama/qwen2.5:1.5b", "Qwen 2.5 1.5B", "ollama", "Lightweight", true),
            new ModelInfo("ollama/qwen2.5:7b", "Qwen 2.5 7B", "ollama", "Balanced", true),
            new ModelInfo("ollama/qwen2.5-coder:7b", "Qwen 2.5 Coder 7B", "ollama", "Code specialist", true),
            
            // Phi (Microsoft)
            new ModelInfo("ollama/phi4:14b", "Phi-4 14B", "ollama", "Microsoft latest", true),
            new ModelInfo("ollama/phi3:3.8b", "Phi-3 Mini 3.8B", "ollama", "Small but capable", true),
            
            // DeepSeek
            new ModelInfo("ollama/deepseek-r1:7b", "DeepSeek R1 7B", "ollama", "Reasoning model", true),
            new ModelInfo("ollama/deepseek-coder-v2:16b", "DeepSeek Coder V2", "ollama", "Code specialist", true),
            
            // Coding models
            new ModelInfo("ollama/codellama:7b", "CodeLlama 7B", "ollama", "Meta code model", true),
            new ModelInfo("ollama/codellama:13b", "CodeLlama 13B", "ollama", "Better coding", true),
            
            // Vision models
            new ModelInfo("ollama/llava:7b", "LLaVA 7B", "ollama", "Vision model", true),
            new ModelInfo("ollama/moondream:1.8b", "Moondream 1.8B", "ollama", "Tiny vision model", true)
        );
    }
}
