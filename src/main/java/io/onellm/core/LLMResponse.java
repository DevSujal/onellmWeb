package io.onellm.core;

import java.util.Objects;

/**
 * Represents a response from an LLM provider.
 */
public class LLMResponse {
    
    private final String id;
    private final String model;
    private final String content;
    private final String finishReason;
    private final Usage usage;
    private final String provider;
    private final long latencyMs;
    
    private LLMResponse(Builder builder) {
        this.id = builder.id;
        this.model = builder.model;
        this.content = builder.content;
        this.finishReason = builder.finishReason;
        this.usage = builder.usage;
        this.provider = builder.provider;
        this.latencyMs = builder.latencyMs;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getId() {
        return id;
    }
    
    public String getModel() {
        return model;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public long getLatencyMs() {
        return latencyMs;
    }
    
    /**
     * Builder for LLMResponse.
     */
    public static class Builder {
        private String id;
        private String model;
        private String content;
        private String finishReason;
        private Usage usage;
        private String provider;
        private long latencyMs;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }
        
        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }
        
        public Builder usage(int promptTokens, int completionTokens) {
            this.usage = new Usage(promptTokens, completionTokens);
            return this;
        }
        
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }
        
        public LLMResponse build() {
            return new LLMResponse(this);
        }
    }
    
    @Override
    public String toString() {
        return "LLMResponse{id='" + id + "', model='" + model + "', provider='" + provider + 
               "', content='" + (content != null && content.length() > 50 ? 
               content.substring(0, 50) + "..." : content) + "', latencyMs=" + latencyMs + "}";
    }
}
