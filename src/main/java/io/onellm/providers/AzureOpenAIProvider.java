package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for Azure OpenAI Service.
 * Supports: Azure-hosted GPT-4, GPT-3.5, and other OpenAI models.
 */
public class AzureOpenAIProvider extends BaseProvider {
    
    private static final String API_VERSION = "2024-02-01";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "azure/", "azure-"
    );
    
    private final String deploymentName;
    private final String apiVersion;
    
    /**
     * Creates an Azure OpenAI provider.
     *
     * @param apiKey         Azure OpenAI API key
     * @param resourceName   Azure resource name (e.g., "my-openai-resource")
     * @param deploymentName Deployment name for the model
     */
    public AzureOpenAIProvider(String apiKey, String resourceName, String deploymentName) {
        this(apiKey, resourceName, deploymentName, API_VERSION);
    }
    
    public AzureOpenAIProvider(String apiKey, String resourceName, String deploymentName, String apiVersion) {
        super(apiKey, "https://" + resourceName + ".openai.azure.com/openai/deployments/" + deploymentName);
        this.deploymentName = deploymentName;
        this.apiVersion = apiVersion;
    }
    
    @Override
    public String getName() {
        return "azure";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    public boolean supportsModel(String modelName) {
        // Azure supports any model through its deployment
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.startsWith("azure/") || lower.startsWith("azure-") || 
               lower.equals(deploymentName.toLowerCase());
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        return baseUrl + "/chat/completions?api-version=" + apiVersion;
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
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
            throw new LLMException("azure", "No choices in response", 0);
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
                .model(deploymentName)
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
