package io.onellm;

import io.onellm.core.*;
import io.onellm.exception.*;
import io.onellm.providers.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * OneLLM - Unified interface for calling any LLM API.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * OneLLM llm = OneLLM.builder()
 *     .openai("sk-...")
 *     .anthropic("sk-ant-...")
 *     .google("AIza...")
 *     .build();
 * 
 * // The model name automatically routes to the correct provider
 * LLMResponse response = llm.complete(
 *     LLMRequest.builder()
 *         .model("gpt-4")
 *         .user("Hello!")
 *         .build()
 * );
 * }</pre>
 */
public class OneLLM implements AutoCloseable {
    
    private final List<LLMProvider> providers;
    private final Map<String, LLMProvider> providersByName;
    private final Map<String, LLMProvider> modelToProviderCache;
    
    private OneLLM(List<LLMProvider> providers) {
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
        this.providersByName = new HashMap<>();
        this.modelToProviderCache = new HashMap<>();
        
        for (LLMProvider provider : providers) {
            providersByName.put(provider.getName().toLowerCase(), provider);
        }
    }
    
    /**
     * Creates a new OneLLM builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Sends a completion request, automatically routing to the correct provider.
     */
    public LLMResponse complete(LLMRequest request) throws LLMException {
        LLMProvider provider = findProvider(request.getModel());
        return provider.complete(request);
    }
    
    /**
     * Sends an async completion request.
     */
    public CompletableFuture<LLMResponse> completeAsync(LLMRequest request) {
        LLMProvider provider = findProvider(request.getModel());
        return provider.completeAsync(request);
    }
    
    /**
     * Sends a streaming completion request.
     */
    public void streamComplete(LLMRequest request, StreamHandler handler) throws LLMException {
        LLMProvider provider = findProvider(request.getModel());
        provider.streamComplete(request, handler);
    }
    
    /**
     * Gets a specific provider by name.
     */
    public Optional<LLMProvider> getProvider(String name) {
        return Optional.ofNullable(providersByName.get(name.toLowerCase()));
    }
    
    /**
     * Gets all configured providers.
     */
    public List<LLMProvider> getProviders() {
        return providers;
    }
    
    /**
     * Lists all configured provider names.
     */
    public List<String> listProviders() {
        List<String> names = new ArrayList<>();
        for (LLMProvider provider : providers) {
            names.add(provider.getName());
        }
        return names;
    }
    
    /**
     * Finds the provider that supports the given model.
     */
    private LLMProvider findProvider(String model) throws LLMException {
        if (model == null || model.isEmpty()) {
            throw new LLMException("Model name cannot be null or empty");
        }
        
        // Check cache first
        LLMProvider cached = modelToProviderCache.get(model.toLowerCase());
        if (cached != null) {
            return cached;
        }
        
        // Check if model has explicit provider prefix (e.g., "openai/gpt-4")
        if (model.contains("/")) {
            String providerName = model.substring(0, model.indexOf("/"));
            LLMProvider provider = providersByName.get(providerName.toLowerCase());
            if (provider != null) {
                modelToProviderCache.put(model.toLowerCase(), provider);
                return provider;
            }
        }
        
        // Find provider that supports this model
        for (LLMProvider provider : providers) {
            if (provider.supportsModel(model)) {
                modelToProviderCache.put(model.toLowerCase(), provider);
                return provider;
            }
        }
        
        throw new ModelNotFoundException(model);
    }
    
    @Override
    public void close() {
        // Providers may have resources to clean up
        for (LLMProvider provider : providers) {
            if (provider instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) provider).close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    /**
     * Builder for constructing OneLLM instances.
     */
    public static class Builder {
        private final List<LLMProvider> providers = new ArrayList<>();
        
        /**
         * Adds OpenAI provider.
         */
        public Builder openai(String apiKey) {
            providers.add(new OpenAIProvider(apiKey));
            return this;
        }
        
        /**
         * Adds OpenAI provider with custom base URL.
         */
        public Builder openai(String apiKey, String baseUrl) {
            providers.add(new OpenAIProvider(apiKey, baseUrl));
            return this;
        }
        
        /**
         * Adds Anthropic provider.
         */
        public Builder anthropic(String apiKey) {
            providers.add(new AnthropicProvider(apiKey));
            return this;
        }
        
        /**
         * Adds Google Gemini provider.
         */
        public Builder google(String apiKey) {
            providers.add(new GoogleProvider(apiKey));
            return this;
        }
        
        /**
         * Adds Azure OpenAI provider.
         */
        public Builder azure(String apiKey, String resourceName, String deploymentName) {
            providers.add(new AzureOpenAIProvider(apiKey, resourceName, deploymentName));
            return this;
        }
        
        /**
         * Adds Groq provider.
         */
        public Builder groq(String apiKey) {
            providers.add(new GroqProvider(apiKey));
            return this;
        }
        
        /**
         * Adds Cerebras provider.
         */
        public Builder cerebras(String apiKey) {
            providers.add(new CerebrasProvider(apiKey));
            return this;
        }
        
        /**
         * Adds Ollama provider with default localhost URL.
         */
        public Builder ollama() {
            providers.add(new OllamaProvider());
            return this;
        }
        
        /**
         * Adds Ollama provider with custom URL.
         */
        public Builder ollama(String baseUrl) {
            providers.add(new OllamaProvider(baseUrl));
            return this;
        }
        
        /**
         * Adds OpenRouter provider.
         */
        public Builder openRouter(String apiKey) {
            providers.add(new OpenRouterProvider(apiKey));
            return this;
        }
        
        /**
         * Adds OpenRouter provider with site info.
         */
        public Builder openRouter(String apiKey, String siteName, String siteUrl) {
            providers.add(new OpenRouterProvider(apiKey, siteName, siteUrl));
            return this;
        }
        
        /**
         * Adds xAI provider.
         */
        public Builder xai(String apiKey) {
            providers.add(new XAIProvider(apiKey));
            return this;
        }
        
        /**
         * Adds GitHub Copilot provider.
         */
        public Builder copilot(String apiKey) {
            providers.add(new CopilotProvider(apiKey));
            return this;
        }
        
        /**
         * Adds Hugging Face provider.
         */
        public Builder huggingface(String apiKey) {
            providers.add(new HuggingFaceProvider(apiKey));
            return this;
        }
        
        /**
         * Adds Hugging Face provider with custom base URL (for dedicated endpoints).
         */
        public Builder huggingface(String apiKey, String baseUrl) {
            providers.add(new HuggingFaceProvider(apiKey, baseUrl));
            return this;
        }
        
        /**
         * Adds a custom provider.
         */
        public Builder provider(LLMProvider provider) {
            providers.add(provider);
            return this;
        }
        
        /**
         * Builds the OneLLM instance.
         */
        public OneLLM build() {
            if (providers.isEmpty()) {
                throw new IllegalStateException("At least one provider must be configured");
            }
            return new OneLLM(providers);
        }
    }
}
