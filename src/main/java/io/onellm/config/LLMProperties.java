package io.onellm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for LLM providers.
 * All API keys can be set via environment variables or application.properties.
 */
@Component
@ConfigurationProperties(prefix = "onellm")
public class LLMProperties {
    
    private OpenAIConfig openai = new OpenAIConfig();
    private AnthropicConfig anthropic = new AnthropicConfig();
    private GoogleConfig google = new GoogleConfig();
    private AzureConfig azure = new AzureConfig();
    private GroqConfig groq = new GroqConfig();
    private CerebrasConfig cerebras = new CerebrasConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private OpenRouterConfig openrouter = new OpenRouterConfig();
    private XAIConfig xai = new XAIConfig();
    private CopilotConfig copilot = new CopilotConfig();
    
    // Getters and Setters
    public OpenAIConfig getOpenai() { return openai; }
    public void setOpenai(OpenAIConfig openai) { this.openai = openai; }
    
    public AnthropicConfig getAnthropic() { return anthropic; }
    public void setAnthropic(AnthropicConfig anthropic) { this.anthropic = anthropic; }
    
    public GoogleConfig getGoogle() { return google; }
    public void setGoogle(GoogleConfig google) { this.google = google; }
    
    public AzureConfig getAzure() { return azure; }
    public void setAzure(AzureConfig azure) { this.azure = azure; }
    
    public GroqConfig getGroq() { return groq; }
    public void setGroq(GroqConfig groq) { this.groq = groq; }
    
    public CerebrasConfig getCerebras() { return cerebras; }
    public void setCerebras(CerebrasConfig cerebras) { this.cerebras = cerebras; }
    
    public OllamaConfig getOllama() { return ollama; }
    public void setOllama(OllamaConfig ollama) { this.ollama = ollama; }
    
    public OpenRouterConfig getOpenrouter() { return openrouter; }
    public void setOpenrouter(OpenRouterConfig openrouter) { this.openrouter = openrouter; }
    
    public XAIConfig getXai() { return xai; }
    public void setXai(XAIConfig xai) { this.xai = xai; }
    
    public CopilotConfig getCopilot() { return copilot; }
    public void setCopilot(CopilotConfig copilot) { this.copilot = copilot; }
    
    // Nested configuration classes
    public static class OpenAIConfig {
        private String apiKey;
        private String baseUrl;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class AnthropicConfig {
        private String apiKey;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class GoogleConfig {
        private String apiKey;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class AzureConfig {
        private String apiKey;
        private String resourceName;
        private String deploymentName;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public String getResourceName() { return resourceName; }
        public void setResourceName(String resourceName) { this.resourceName = resourceName; }
        public String getDeploymentName() { return deploymentName; }
        public void setDeploymentName(String deploymentName) { this.deploymentName = deploymentName; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class GroqConfig {
        private String apiKey;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class CerebrasConfig {
        private String apiKey;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private boolean enabled = true;  // Enabled by default for local development
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class OpenRouterConfig {
        private String apiKey;
        private String siteName;
        private String siteUrl;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public String getSiteName() { return siteName; }
        public void setSiteName(String siteName) { this.siteName = siteName; }
        public String getSiteUrl() { return siteUrl; }
        public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class XAIConfig {
        private String apiKey;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    public static class CopilotConfig {
        private String apiKey;
        private boolean enabled = false;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; this.enabled = apiKey != null && !apiKey.isEmpty(); }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
