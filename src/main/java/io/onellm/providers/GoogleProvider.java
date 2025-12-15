package io.onellm.providers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.onellm.core.LLMRequest;
import io.onellm.core.LLMResponse;
import io.onellm.core.Message;
import io.onellm.core.StreamHandler;
import io.onellm.core.Usage;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.LLMException;

/**
 * Provider for Google's Gemini models.
 * Supports: gemini-pro, gemini-ultra, gemini-1.5-pro, gemini-2.0-flash, etc.
 */
public class GoogleProvider extends BaseProvider {
    
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final List<String> MODEL_PREFIXES = Arrays.asList(
            "gemini/", "google/"
    );
    
    public GoogleProvider(String apiKey) {
        super(apiKey, DEFAULT_BASE_URL);
    }
    
    public GoogleProvider(String apiKey, String baseUrl) {
        super(apiKey, baseUrl);
    }
    
    @Override
    public String getName() {
        return "google";
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
        // Model is in the URL for Google's API
        return baseUrl;
    }
    
    protected String getEndpointForModel(String model, boolean stream) {
        String normalizedModel = normalizeModelId(model);
        String modelName = normalizedModel.startsWith("models/") ? normalizedModel : "models/" + normalizedModel;
        String action = stream ? "streamGenerateContent" : "generateContent";
        String trimmedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmedBaseUrl + "/" + modelName + ":" + action + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    }

    private String normalizeModelId(String model) {
        if (model == null) return "";
        String normalized = model.trim();
        if (normalized.isEmpty()) return normalized;

        // Accept explicit provider prefixes (e.g., "google/gemini-2.0-flash" or "gemini/gemini-2.0-flash")
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("google/") || lower.startsWith("gemini/")) {
            normalized = normalized.substring(normalized.indexOf('/') + 1);
            lower = normalized.toLowerCase(Locale.ROOT);
        }

        // Accept "models/<id>" input; we add "models/" back during URL building
        if (lower.startsWith("models/")) {
            normalized = normalized.substring("models/".length());
        }

        return normalized;
    }
    
    @Override
    public LLMResponse complete(LLMRequest request) throws LLMException {
        long startTime = System.currentTimeMillis();
        
        Map<String, Object> body = buildRequestBody(request);
        String endpoint = getEndpointForModel(request.getModel(), false);
        
        logger.debug("Sending request to Google Gemini for model {}", request.getModel());
        
        JsonObject response = httpClient.post(endpoint, getHeaders(), body);
        
        long latencyMs = System.currentTimeMillis() - startTime;
        return parseResponse(response, request.getModel(), latencyMs);
    }
    
    @Override
    public void streamComplete(LLMRequest request, StreamHandler handler) throws LLMException {
        Map<String, Object> body = buildRequestBody(request);
        String endpoint = getEndpointForModel(request.getModel(), true);
        
        StringBuilder fullContent = new StringBuilder();
        long startTime = System.currentTimeMillis();
        
        httpClient.postStream(endpoint, getHeaders(), body,
            line -> {
                String chunk = parseGoogleStreamChunk(line);
                if (chunk != null && !chunk.isEmpty()) {
                    fullContent.append(chunk);
                    handler.onChunk(chunk);
                }
            },
            error -> handler.onError(error)
        );
        
        long latencyMs = System.currentTimeMillis() - startTime;
        LLMResponse response = LLMResponse.builder()
                .content(fullContent.toString())
                .model(request.getModel())
                .provider(getName())
                .latencyMs(latencyMs)
                .build();
        handler.onComplete(response);
    }
    
    @Override
    protected Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Convert messages to Gemini's format
        List<Map<String, Object>> contents = new ArrayList<>();
        String systemInstruction = null;
        
        for (Message msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                systemInstruction = msg.getContent();
            } else {
                Map<String, Object> content = new HashMap<>();
                content.put("role", "user".equals(msg.getRole()) ? "user" : "model");
                
                List<Map<String, String>> parts = new ArrayList<>();
                Map<String, String> textPart = new HashMap<>();
                textPart.put("text", msg.getContent());
                parts.add(textPart);
                content.put("parts", parts);
                
                contents.add(content);
            }
        }
        
        body.put("contents", contents);
        
        if (systemInstruction != null) {
            Map<String, Object> sysInstr = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> textPart = new HashMap<>();
            textPart.put("text", systemInstruction);
            parts.add(textPart);
            sysInstr.put("parts", parts);
            body.put("systemInstruction", sysInstr);
        }
        
        // Generation config
        Map<String, Object> genConfig = new HashMap<>();
        if (request.getTemperature() != null) {
            genConfig.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            genConfig.put("maxOutputTokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            genConfig.put("topP", request.getTopP());
        }
        if (request.getStop() != null && !request.getStop().isEmpty()) {
            genConfig.put("stopSequences", request.getStop());
        }
        
        if (!genConfig.isEmpty()) {
            body.put("generationConfig", genConfig);
        }
        
        return body;
    }
    
    @Override
    protected LLMResponse parseResponse(JsonObject response, String model, long latencyMs) {
        JsonArray candidates = response.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new LLMException("google", "No candidates in response", 0);
        }
        
        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject content = candidate.getAsJsonObject("content");
        JsonArray parts = content.getAsJsonArray("parts");
        
        StringBuilder textContent = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            JsonObject part = parts.get(i).getAsJsonObject();
            if (part.has("text")) {
                textContent.append(part.get("text").getAsString());
            }
        }
        
        String finishReason = candidate.has("finishReason") 
                ? candidate.get("finishReason").getAsString() : null;
        
        Usage usage = null;
        if (response.has("usageMetadata")) {
            JsonObject usageObj = response.getAsJsonObject("usageMetadata");
            usage = new Usage(
                    usageObj.get("promptTokenCount").getAsInt(),
                    usageObj.get("candidatesTokenCount").getAsInt(),
                    usageObj.get("totalTokenCount").getAsInt()
            );
        }
        
        return LLMResponse.builder()
                .model(model)
                .content(textContent.toString())
                .finishReason(finishReason)
                .usage(usage)
                .provider(getName())
                .latencyMs(latencyMs)
                .build();
    }
    
    @Override
    protected String extractContentFromStreamChunk(JsonObject chunk) {
        // Not used - Google uses different streaming format
        return null;
    }
    
    private String parseGoogleStreamChunk(String line) {
        try {
            // Google returns JSON array elements
            if (line.startsWith("[") || line.startsWith(",") || line.startsWith("]")) {
                if (line.startsWith(",")) line = line.substring(1);
                if (line.equals("[") || line.equals("]")) return null;
            }
            
            JsonObject chunk = com.google.gson.JsonParser.parseString(line).getAsJsonObject();
            JsonArray candidates = chunk.getAsJsonArray("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                if (content != null) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && !parts.isEmpty()) {
                        JsonObject part = parts.get(0).getAsJsonObject();
                        if (part.has("text")) {
                            return part.get("text").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.trace("Failed to parse Google stream chunk: {}", line);
        }
        return null;
    }
    
    @Override
    protected List<ModelInfo> getStaticModels() {
        return Arrays.asList(
            // Gemini 2.0 family
            new ModelInfo("google/gemini-2.0-flash", "Gemini 2.0 Flash", "google", "Latest & fastest"),
            new ModelInfo("google/gemini-2.0-flash-exp", "Gemini 2.0 Flash Exp", "google", "Experimental"),
            new ModelInfo("google/gemini-2.0-flash-thinking-exp", "Gemini 2.0 Flash Thinking", "google", "Reasoning mode"),
            
            // Gemini 1.5 family
            new ModelInfo("google/gemini-1.5-pro", "Gemini 1.5 Pro", "google", "Most capable"),
            new ModelInfo("google/gemini-1.5-pro-latest", "Gemini 1.5 Pro Latest", "google", "Latest Pro"),
            new ModelInfo("google/gemini-1.5-pro-002", "Gemini 1.5 Pro 002", "google", "Stable version"),
            new ModelInfo("google/gemini-1.5-flash", "Gemini 1.5 Flash", "google", "Fast"),
            new ModelInfo("google/gemini-1.5-flash-latest", "Gemini 1.5 Flash Latest", "google", "Latest Flash"),
            new ModelInfo("google/gemini-1.5-flash-002", "Gemini 1.5 Flash 002", "google", "Stable Flash"),
            new ModelInfo("google/gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", "google", "Lightweight Flash"),
            
            // Gemini 1.0 family
            new ModelInfo("google/gemini-1.0-pro", "Gemini 1.0 Pro", "google", "Balanced"),
            new ModelInfo("google/gemini-pro", "Gemini Pro", "google", "Alias for 1.0 Pro"),
            new ModelInfo("google/gemini-pro-vision", "Gemini Pro Vision", "google", "Vision capabilities")
        );
    }
}
