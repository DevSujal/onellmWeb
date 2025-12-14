package io.onellm.service;

import javavutil.Arrays
import javavutil.List
import javavutil.Map


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
ioonellmcore.LLMProvider
import iononellm.exception.ModelNotFoundException.LLMProvider;
import iononellm.providers.AnthropicProvidereption.ModelNotFoundException;
import io.onellm.providers.AzureOpenAIProvider;
import io.onellm.providers.CerebrasProvider;
import io.onellm.providers.CopilotProvider;
import io.onellm.providers.GoogleProvider;
import io.onellm.providers.GroqProvider;
import io.onellm.providers.HuggingFaceProvider;
import io.onellm.providers.OllamaProvider;
import io.onellm.providers.OpenAIProvider;
import io.onellm.providers.OpenRouterProvider;
import io.onellm.providers.XAIProvider;
import io.onellm.providers.AnthropicProvider;
import io.onellm.providers.AzureOpenAIProvider;
import io.onellm.providers.CerebrasProvider;
import io.onellm.providers.CopilotProvider;
import io.onellm.providers.GoogleProvider;
import io.onellm.providers.GroqProvider;
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
        Map.entry("huggingface", Arrays.asList("huggingface/", "hf/", "meta-llama/", "mistralai/", "microsoft/phi", "Qwen/"))
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
        
        // Validate API key is required for all providers except Ollama
        if (!"ollama".equals(providerName) && (apiKey == null || apiKey.isBlank())) {
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
            "cerebras", "ollama", "openrouter", "xai", "copilot", "huggingface"
        );
    }
}
