package io.onellm.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.onellm.core.LLMProvider;
import io.onellm.dto.ModelInfo;
import io.onellm.exception.ModelNotFoundException;
import io.onellm.providers.AnthropicProvider;
import io.onellm.providers.AzureOpenAIProvider;
import io.onellm.providers.CerebrasProvider;
import io.onellm.providers.CopilotProvider;
import io.onellm.providers.GoogleProvider;
import io.onellm.providers.GroqProvider;
import io.onellm.providers.FreeLLMProvider;
import io.onellm.providers.HuggingFaceProvider;
import io.onellm.providers.OllamaProvider;
import io.onellm.providers.OpenAIProvider;
import io.onellm.providers.OpenRouterProvider;
import io.onellm.providers.XAIProvider;

/**
 * Factory for creating LLM providers on-demand with user-provided API keys.
 * This enables multi-tenant usage where each request can use different API credentials.
 */
@Service
public class ProviderFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ProviderFactory.class);
    
    // Model prefix mappings for auto-detection
    private static final Map<String, List<String>> PROVIDER_PREFIXES = Map.ofEntries(
        Map.entry("openai", Arrays.asList("gpt-4", "gpt-3.5", "o1", "o3", "chatgpt")),
        Map.entry("anthropic", Arrays.asList("claude-3", "claude-4", "claude-2", "claude-instant")),
        Map.entry("google", Arrays.asList("gemini", "models/gemini")),
        Map.entry("groq", Arrays.asList("llama-3", "mixtral", "gemma")),
        Map.entry("cerebras", Arrays.asList("cerebras", "llama3.1-8b", "llama3.1-70b", "llama-3.3-70b", "gpt-oss")),
        Map.entry("ollama", Arrays.asList("ollama/", "llama2", "codellama", "phi")),
        Map.entry("xai", Arrays.asList("grok")),
        Map.entry("copilot", Arrays.asList("copilot")),
        Map.entry("huggingface", Arrays.asList("huggingface/", "hf/", "meta-llama/", "mistralai/", "microsoft/phi", "Qwen/")),
        Map.entry("freellm", Arrays.asList("freellm/", "free/", "TinyLlama/"))
    );
    
    /**
     * Creates an LLM provider based on the model name and user-provided credentials.
     *
     * @param model The model name (used to detect provider)
     * @param apiKey The user's API key
     * @param baseUrl Optional custom base URL
     * @param azureResourceName Azure resource name (required for Azure)
     * @param azureDeploymentName Azure deployment name (required for Azure)
     * @param openRouterSiteName OpenRouter site name (optional)
     * @param openRouterSiteUrl OpenRouter site URL (optional)
     * @return The configured LLM provider
     * @throws ModelNotFoundException if no provider matches the model
     */
    public LLMProvider createProvider(
            String model,
            String apiKey,
            String baseUrl,
            String azureResourceName,
            String azureDeploymentName,
            String openRouterSiteName,
            String openRouterSiteUrl) {
        
        String providerName = detectProvider(model);
        logger.debug("Creating {} provider for model: {}", providerName, model);
        
        // Validate API key is required for all providers except Ollama and FreeLLM
        if (!"ollama".equals(providerName) && !"freellm".equals(providerName) && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException(
                "API key is required for " + providerName + " provider");
        }
        
        return switch (providerName) {
            case "openai" -> baseUrl != null && !baseUrl.isEmpty() 
                    ? new OpenAIProvider(apiKey, baseUrl) 
                    : new OpenAIProvider(apiKey);
            case "anthropic" -> new AnthropicProvider(apiKey);
                case "google" -> baseUrl != null && !baseUrl.isEmpty()
                    ? new GoogleProvider(apiKey, baseUrl)
                    : new GoogleProvider(apiKey);
            case "azure" -> {
                if (azureResourceName == null || azureDeploymentName == null) {
                    throw new IllegalArgumentException(
                        "Azure provider requires azureResourceName and azureDeploymentName");
                }
                yield new AzureOpenAIProvider(apiKey, azureResourceName, azureDeploymentName);
            }
            case "groq" -> new GroqProvider(apiKey);
            case "cerebras" -> new CerebrasProvider(apiKey);
            case "ollama" -> baseUrl != null && !baseUrl.isEmpty() 
                    ? new OllamaProvider(baseUrl) 
                    : new OllamaProvider();
            case "openrouter" -> openRouterSiteName != null 
                    ? new OpenRouterProvider(apiKey, openRouterSiteName, openRouterSiteUrl)
                    : new OpenRouterProvider(apiKey);
            case "xai" -> new XAIProvider(apiKey);
            case "copilot" -> new CopilotProvider(apiKey);
            case "huggingface" -> baseUrl != null && !baseUrl.isEmpty()
                    ? new HuggingFaceProvider(apiKey, baseUrl)
                    : new HuggingFaceProvider(apiKey);
            case "freellm" -> baseUrl != null && !baseUrl.isEmpty()
                    ? new FreeLLMProvider(baseUrl)
                    : new FreeLLMProvider();
            default -> throw new ModelNotFoundException(model);
        };
    }
    
    /**
     * Detects the provider based on model name or explicit prefix.
     */
    public String detectProvider(String model) {
        if (model == null || model.isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
        
        String lowerModel = model.toLowerCase();
        
        // Check for explicit provider prefix (e.g., "openai/gpt-4", "azure/my-deployment")
        if (model.contains("/")) {
            String prefix = model.substring(0, model.indexOf("/")).toLowerCase();
            // Check if it's a known provider
            if (PROVIDER_PREFIXES.containsKey(prefix) || 
                prefix.equals("azure") || 
                prefix.equals("openrouter")) {
                return prefix;
            }
        }
        
        // Auto-detect based on model name patterns
        for (Map.Entry<String, List<String>> entry : PROVIDER_PREFIXES.entrySet()) {
            for (String modelPrefix : entry.getValue()) {
                if (lowerModel.startsWith(modelPrefix.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        
        // Default to openrouter for unknown models (it supports many models)
        logger.warn("Unknown model pattern '{}', defaulting to openrouter", model);
        return "openrouter";
    }
    
    /**
     * Gets a list of all supported providers.
     */
    public List<String> getSupportedProviders() {
        return Arrays.asList(
            "openai", "anthropic", "google", "azure", "groq", 
            "cerebras", "ollama", "openrouter", "xai", "copilot", "huggingface", "freellm"
        );
    }
    
    /**
     * Gets available models for a specific provider.
     * For providers that support dynamic fetching (OpenAI, Groq, etc.), 
     * this will fetch the latest models if an API key is provided.
     *
     * @param providerName The provider name
     * @param apiKey Optional API key for dynamic fetching
     * @param baseUrl Optional custom base URL
     * @return List of available models for the provider
     */
    public List<ModelInfo> getModelsForProvider(String providerName, String apiKey, String baseUrl) {
        try {
            LLMProvider provider = createProviderForModels(providerName, apiKey, baseUrl);
            return provider.getAvailableModels();
        } catch (Exception e) {
            logger.warn("Failed to get models for provider {}: {}", providerName, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets all available models from all providers.
     * Uses static lists for all providers (no API key required).
     *
     * @return List of all available models
     */
    public List<ModelInfo> getAllStaticModels() {
        List<ModelInfo> allModels = new ArrayList<>();
        
        for (String providerName : getSupportedProviders()) {
            try {
                LLMProvider provider = createProviderForModels(providerName, null, null);
                allModels.addAll(provider.getAvailableModels());
            } catch (Exception e) {
                logger.debug("Skipping provider {} for static models: {}", providerName, e.getMessage());
            }
        }
        
        return allModels;
    }
    
    /**
     * Gets models from multiple providers with API keys for dynamic fetching.
     *
     * @param providerApiKeys Map of provider name to API key
     * @return List of all available models
     */
    public List<ModelInfo> getModelsWithApiKeys(Map<String, String> providerApiKeys) {
        List<ModelInfo> allModels = new ArrayList<>();
        
        for (String providerName : getSupportedProviders()) {
            try {
                String apiKey = providerApiKeys.get(providerName);
                LLMProvider provider = createProviderForModels(providerName, apiKey, null);
                allModels.addAll(provider.getAvailableModels());
            } catch (Exception e) {
                logger.debug("Failed to get models for provider {}: {}", providerName, e.getMessage());
            }
        }
        
        return allModels;
    }
    
    /**
     * Creates a provider instance for fetching models.
     * Uses dummy/empty credentials when not provided.
     */
    private LLMProvider createProviderForModels(String providerName, String apiKey, String baseUrl) {
        // For providers that don't require API key
        if ("ollama".equals(providerName)) {
            return baseUrl != null && !baseUrl.isEmpty() 
                    ? new OllamaProvider(baseUrl) 
                    : new OllamaProvider();
        }
        if ("freellm".equals(providerName)) {
            return baseUrl != null && !baseUrl.isEmpty() 
                    ? new FreeLLMProvider(baseUrl) 
                    : new FreeLLMProvider();
        }
        
        // For providers that require API key, use provided or dummy key
        String effectiveApiKey = apiKey != null && !apiKey.isEmpty() ? apiKey : "dummy-key-for-static-models";
        
        return switch (providerName) {
            case "openai" -> baseUrl != null && !baseUrl.isEmpty() 
                    ? new OpenAIProvider(effectiveApiKey, baseUrl) 
                    : new OpenAIProvider(effectiveApiKey);
            case "anthropic" -> new AnthropicProvider(effectiveApiKey);
            case "google" -> baseUrl != null && !baseUrl.isEmpty()
                    ? new GoogleProvider(effectiveApiKey, baseUrl)
                    : new GoogleProvider(effectiveApiKey);
            case "azure" -> new AzureOpenAIProvider(effectiveApiKey, "dummy-resource", "dummy-deployment");
            case "groq" -> new GroqProvider(effectiveApiKey);
            case "cerebras" -> new CerebrasProvider(effectiveApiKey);
            case "openrouter" -> new OpenRouterProvider(effectiveApiKey);
            case "xai" -> new XAIProvider(effectiveApiKey);
            case "copilot" -> new CopilotProvider(effectiveApiKey);
            case "huggingface" -> baseUrl != null && !baseUrl.isEmpty()
                    ? new HuggingFaceProvider(effectiveApiKey, baseUrl)
                    : new HuggingFaceProvider(effectiveApiKey);
            default -> throw new ModelNotFoundException("Unknown provider: " + providerName);
        };
    }
}
