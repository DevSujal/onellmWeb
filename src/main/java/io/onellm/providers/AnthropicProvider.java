package io.onellm.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.onellm.core.LLMRequest;
import io.onellm.core.LLMResponse;
import io.onellm.core.Message;
import io.onellm.core.Usage;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

/**
 * Provider for Anthropic's Claude models.
 * Supports: claude-3-opus, claude-3-sonnet, claude-3-haiku, claude-3.5-sonnet, claude-4, etc.
 */
public class AnthropicProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String API_VERSION = "2023-06-01";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "claude/", "anthropic/"
    );
    
    public AnthropicProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public AnthropicProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "anthropic";
    }
    
    @Override
    public List<String> getModelPrefixes() {
        return MODEL_PREFIXES;
    }
    
    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", API_VERSION);
        headers.put("Content-Type", "application/json");
        return headers;
    }
    
    @Override
    protected String getCompletionEndpoint() {
        return baseUrl + "/messages";
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        
        // Anthropic uses a different message format - system message is separate
        List<Message> messages = request.getMessages();
        String systemPrompt = null;
        List<Map<String, String>> anthropicMessages = new ArrayList<>();
        
        for (Message msg : messages) {
            if ("system".equals(msg.getRole())) {
                systemPrompt = msg.getContent();
            } else {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                anthropicMessages.add(m);
            }
        }
        
        if (systemPrompt != null) {
            body.put("system", systemPrompt);
        }
        body.put("messages", anthropicMessages);
        
        // Anthropic requires max_tokens
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            body.put("stop_sequences", request.getStop());
        }
        if (request.isStream()) {
            body.put("stream", true);
        }
        
        return body;
    }
    
    @Override
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        JsonArray content = response.getAsJsonArray("content");
        if (content == null || content.isEmpty()) {
            throw new LLMException("anthropic", "No content in response", 0);
        }
        
        StringBuilder textContent = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JsonObject block = content.get(i).getAsJsonObject();
            if ("text".equals(block.get("type").getAsString())) {
                textContent.append(block.get("text").getAsString());
            }
        }
        
        String stopReason = response.has("stop_reason") && !response.get("stop_reason").isJsonNull()
                ? response.get("stop_reason").getAsString() : null;
        
        Usage usage = null;
        if (response.has("usage")) {
            JsonObject usageObj = response.getAsJsonObject("usage");
            usage = new Usage(
                    usageObj.get("input_tokens").getAsInt(),
                    usageObj.get("output_tokens").getAsInt()
            );
        }
        
        return LLMResponse.builder()
                .id(response.has("id") ? response.get("id").getAsString() : null)
                .model(response.has("model") ? response.get("model").getAsString() : model)
                .content(textContent.toString())
                .finishReason(stopReason)
                .usage(usage)
                .provider(getName())
                .latencyMs(latencyMs)
                .build();
    }
    
    @Override
    protected String extractContentFromStreamChunk(JsonObject chunk) {
        String type = chunk.has("type") ? chunk.get("type").getAsString() : "";
        
        if ("content_block_delta".equals(type)) {
            JsonObject delta = chunk.getAsJsonObject("delta");
            if (delta != null && "text_delta".equals(delta.get("type").getAsString())) {
                return delta.get("text").getAsString();
            }
        }
        return null;
    }
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            // Claude 3.5 family
            new ModelInfo("anthropic/claude-3-5-sonnet-latest", "Claude 3.5 Sonnet", "anthropic", "Best balance"),
            new ModelInfo("anthropic/claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet (Oct 2024)", "anthropic", "Latest Sonnet"),
            new ModelInfo("anthropic/claude-3-5-sonnet-20240620", "Claude 3.5 Sonnet (Jun 2024)", "anthropic", "Original 3.5"),
            new ModelInfo("anthropic/claude-3-5-haiku-latest", "Claude 3.5 Haiku", "anthropic", "Fast & smart"),
            new ModelInfo("anthropic/claude-3-5-haiku-20241022", "Claude 3.5 Haiku (Oct 2024)", "anthropic", "Latest Haiku"),
            
            // Claude 3 family
            new ModelInfo("anthropic/claude-3-opus-latest", "Claude 3 Opus", "anthropic", "Most capable"),
            new ModelInfo("anthropic/claude-3-opus-20240229", "Claude 3 Opus (Feb 2024)", "anthropic", "Original Opus"),
            new ModelInfo("anthropic/claude-3-sonnet-20240229", "Claude 3 Sonnet", "anthropic", "Balanced"),
            new ModelInfo("anthropic/claude-3-haiku-20240307", "Claude 3 Haiku", "anthropic", "Fastest"),
            
            // Legacy Claude
            new ModelInfo("anthropic/claude-2.1", "Claude 2.1", "anthropic", "200K context"),
            new ModelInfo("anthropic/claude-2.0", "Claude 2.0", "anthropic", "Previous gen"),
            new ModelInfo("anthropic/claude-instant-1.2", "Claude Instant 1.2", "anthropic", "Legacy fast")
        );
    }
}
