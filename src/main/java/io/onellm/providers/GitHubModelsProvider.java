package io.onellm.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.onellm.core.*;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

import java.util.*;

/**
 * Provider for GitHub Models API.
 * 
 * <p>GitHub Models provides access to various AI models through Azure's infrastructure.
 * Uses GitHub Personal Access Tokens (PAT) for authentication.</p>
 * 
 * <p>Supported models include:</p>
 * <ul>
 *   <li>OpenAI models: GPT-4o, GPT-4o-mini, o1, o1-mini</li>
 *   <li>Meta Llama models: Llama-3.2, Llama-3.3</li>
 *   <li>Mistral models: Mistral-large, Mistral-small</li>
 *   <li>Cohere models: Command-r, Command-r-plus</li>
 *   <li>AI21 models: Jamba</li>
 *   <li>DeepSeek models: DeepSeek-V2</li>
 * </ul>
 * 
 * <p>Usage: Use model names with "github/" prefix, e.g., "github/gpt-4o"</p>
 * 
 * @see <a href="https://github.com/marketplace/models">GitHub Models Marketplace</a>
 */
public class GitHubModelsProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://models.inference.ai.azure.com";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "github/", "ghmodels/"
    );
    
    public GitHubModelsProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public GitHubModelsProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "github";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        return baseUrl + "/chat/completions";
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Remove github/ prefix if present
        String model = stripProviderPrefix(request.getModel());
        
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
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new LLMException("github", "No choices in response", 0);
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
            // OpenAI models
            new ModelInfo("github/gpt-4o", "GPT-4o (GitHub)", "github", "OpenAI flagship multimodal"),
            new ModelInfo("github/gpt-4o-mini", "GPT-4o Mini (GitHub)", "github", "Fast & affordable"),
            new ModelInfo("github/o1", "O1 (GitHub)", "github", "Advanced reasoning"),
            new ModelInfo("github/o1-preview", "O1 Preview (GitHub)", "github", "Reasoning preview"),
            new ModelInfo("github/o1-mini", "O1 Mini (GitHub)", "github", "Fast reasoning"),
            
            // Meta Llama models
            new ModelInfo("github/Meta-Llama-3.1-8B-Instruct", "Llama 3.1 8B (GitHub)", "github", "Meta Llama 8B"),
            new ModelInfo("github/Meta-Llama-3.1-70B-Instruct", "Llama 3.1 70B (GitHub)", "github", "Meta Llama 70B"),
            new ModelInfo("github/Meta-Llama-3.1-405B-Instruct", "Llama 3.1 405B (GitHub)", "github", "Meta Llama 405B"),
            new ModelInfo("github/Meta-Llama-3.2-11B-Vision-Instruct", "Llama 3.2 11B Vision (GitHub)", "github", "Vision capable"),
            new ModelInfo("github/Meta-Llama-3.2-90B-Vision-Instruct", "Llama 3.2 90B Vision (GitHub)", "github", "Large vision model"),
            new ModelInfo("github/Llama-3.3-70B-Instruct", "Llama 3.3 70B (GitHub)", "github", "Latest Llama"),
            
            // Mistral models
            new ModelInfo("github/Mistral-large", "Mistral Large (GitHub)", "github", "Mistral flagship"),
            new ModelInfo("github/Mistral-large-2411", "Mistral Large 2411 (GitHub)", "github", "Latest Mistral Large"),
            new ModelInfo("github/Mistral-small", "Mistral Small (GitHub)", "github", "Fast Mistral"),
            new ModelInfo("github/Ministral-3B", "Ministral 3B (GitHub)", "github", "Compact Mistral"),
            new ModelInfo("github/Mistral-Nemo", "Mistral Nemo (GitHub)", "github", "Balanced Mistral"),
            
            // Cohere models
            new ModelInfo("github/Cohere-command-r", "Command R (GitHub)", "github", "Cohere reasoning"),
            new ModelInfo("github/Cohere-command-r-plus", "Command R+ (GitHub)", "github", "Enhanced Cohere"),
            new ModelInfo("github/Cohere-command-r-08-2024", "Command R Aug 2024 (GitHub)", "github", "Latest Command R"),
            new ModelInfo("github/Cohere-command-r-plus-08-2024", "Command R+ Aug 2024 (GitHub)", "github", "Latest Command R+"),
            
            // AI21 models
            new ModelInfo("github/AI21-Jamba-1.5-Large", "Jamba 1.5 Large (GitHub)", "github", "AI21 large"),
            new ModelInfo("github/AI21-Jamba-1.5-Mini", "Jamba 1.5 Mini (GitHub)", "github", "AI21 compact"),
            
            // DeepSeek models
            new ModelInfo("github/DeepSeek-R1", "DeepSeek R1 (GitHub)", "github", "DeepSeek reasoning model"),
            
            // Microsoft Phi models
            new ModelInfo("github/Phi-3.5-mini-instruct", "Phi 3.5 Mini (GitHub)", "github", "Microsoft Phi"),
            new ModelInfo("github/Phi-3.5-MoE-instruct", "Phi 3.5 MoE (GitHub)", "github", "Mixture of Experts"),
            new ModelInfo("github/Phi-3.5-vision-instruct", "Phi 3.5 Vision (GitHub)", "github", "Vision capable Phi"),
            new ModelInfo("github/Phi-4", "Phi-4 (GitHub)", "github", "Latest Microsoft Phi")
        );
    }
}
