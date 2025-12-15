package io.onellm.controller;

import io.onellm.core.LLMProvider;
import io.onellm.core.LLMRequest;
import io.onellm.core.LLMResponse;
import io.onellm.core.Message;
import io.onellm.dto.ChatCompletionRequest;
import io.onellm.dto.ChatCompletionResponse;
import io.onellm.dto.MessageDTO;
import io.onellm.dto.ModelInfo;
import io.onellm.service.ProviderFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST Controller for chat completions.
 * Provides endpoints for synchronous and streaming LLM completions.
 * 
 * Users must provide their own API keys in each request.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ProviderFactory providerFactory;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();
    
    public ChatController(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }
    
    /**
     * Synchronous chat completion endpoint.
     * POST /api/chat/completions
     * 
     * Requires user to provide their API key in the request body.
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<ChatCompletionResponse> chatCompletion(
            @Valid @RequestBody ChatCompletionRequest request) {
        
        logger.info("Chat completion request for model: {}", request.getModel());
        
        // Create provider with user's API key
        LLMProvider provider = providerFactory.createProvider(
                request.getModel(),
                request.getApiKey(),
                request.getBaseUrl(),
                request.getAzureResourceName(),
                request.getAzureDeploymentName(),
                request.getOpenRouterSiteName(),
                request.getOpenRouterSiteUrl()
        );
        
        LLMRequest llmRequest = buildLLMRequest(request);
        LLMResponse response = provider.complete(llmRequest);
        
        return ResponseEntity.ok(ChatCompletionResponse.fromLLMResponse(response));
    }
    
    /**
     * Streaming chat completion endpoint using Server-Sent Events.
     * POST /api/chat/completions/stream
     * 
     * Requires user to provide their API key in the request body.
     */
    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatCompletion(
            @Valid @RequestBody ChatCompletionRequest request) {
        
        logger.info("Streaming chat completion request for model: {}", request.getModel());
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout
        
        streamExecutor.execute(() -> {
            try {
                // Create provider with user's API key
                LLMProvider provider = providerFactory.createProvider(
                        request.getModel(),
                        request.getApiKey(),
                        request.getBaseUrl(),
                        request.getAzureResourceName(),
                        request.getAzureDeploymentName(),
                        request.getOpenRouterSiteName(),
                        request.getOpenRouterSiteUrl()
                );
                
                LLMRequest llmRequest = buildLLMRequest(request);
                
                provider.streamComplete(llmRequest, new io.onellm.core.StreamHandler() {
                    @Override
                    public void onChunk(String chunk) {
                        try {
                            Map<String, String> data = new HashMap<>();
                            data.put("content", chunk);
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(data));
                        } catch (IOException e) {
                            logger.error("Error sending SSE chunk", e);
                        }
                    }
                    
                    @Override
                    public void onComplete(LLMResponse response) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(ChatCompletionResponse.fromLLMResponse(response)));
                            emitter.complete();
                        } catch (IOException e) {
                            logger.error("Error completing SSE", e);
                        }
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        logger.error("Stream error", error);
                        emitter.completeWithError(error);
                    }
                });
            } catch (Exception e) {
                logger.error("Error in streaming completion", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * List all supported providers.
     * GET /api/providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> listProviders() {
        List<String> providers = providerFactory.getSupportedProviders();
        
        Map<String, Object> response = new HashMap<>();
        response.put("providers", providers);
        response.put("count", providers.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all available models from all providers (static lists).
     * GET /api/models
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listAllModels() {
        logger.info("Fetching all available models (static)");
        
        List<ModelInfo> models = providerFactory.getAllStaticModels();
        
        Map<String, Object> response = new HashMap<>();
        response.put("models", models);
        response.put("count", models.size());
        response.put("object", "list");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get models for a specific provider.
     * GET /api/models/{provider}
     * Query params: apiKey (optional), baseUrl (optional)
     * 
     * If apiKey is provided, will attempt to fetch dynamic models.
     * Otherwise, returns static list.
     */
    @GetMapping("/models/{provider}")
    public ResponseEntity<Map<String, Object>> listProviderModels(
            @PathVariable String provider,
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String baseUrl) {
        
        logger.info("Fetching models for provider: {}", provider);
        
        List<ModelInfo> models = providerFactory.getModelsForProvider(provider, apiKey, baseUrl);
        
        Map<String, Object> response = new HashMap<>();
        response.put("provider", provider);
        response.put("models", models);
        response.put("count", models.size());
        response.put("dynamic", apiKey != null && !apiKey.isEmpty());
        response.put("object", "list");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get models from multiple providers with API keys for dynamic fetching.
     * POST /api/models
     * 
     * Request body: { "providers": { "openai": "sk-xxx", "groq": "gsk-xxx" } }
     */
    @PostMapping("/models")
    public ResponseEntity<Map<String, Object>> listModelsWithKeys(
            @RequestBody Map<String, Object> request) {
        
        logger.info("Fetching models with API keys");
        
        @SuppressWarnings("unchecked")
        Map<String, String> providerApiKeys = (Map<String, String>) request.getOrDefault("providers", new HashMap<>());
        
        List<ModelInfo> models = providerFactory.getModelsWithApiKeys(providerApiKeys);
        
        Map<String, Object> response = new HashMap<>();
        response.put("models", models);
        response.put("count", models.size());
        response.put("object", "list");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "OneLLM");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Builds an LLMRequest from the REST DTO.
     */
    private LLMRequest buildLLMRequest(ChatCompletionRequest request) {
        LLMRequest.Builder builder = LLMRequest.builder()
                .model(request.getModel());
        
        // Add messages
        for (MessageDTO msg : request.getMessages()) {
            builder.addMessage(new Message(msg.getRole(), msg.getContent()));
        }
        
        // Add optional parameters
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            builder.maxTokens(request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            builder.topP(request.getTopP());
        }
        if (request.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(request.getFrequencyPenalty());
        }
        if (request.getPresencePenalty() != null) {
            builder.presencePenalty(request.getPresencePenalty());
        }
        if (request.getStop() != null) {
            builder.stop(request.getStop());
        }
        if (request.getStream() != null) {
            builder.stream(request.getStream());
        }
        
        return builder.build();
    }
}
