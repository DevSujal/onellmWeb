package io.onellm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a request to an LLM provider.
 * Use the builder pattern to construct requests.
 */
public class LLMRequest {
    
    private final String model;
    private final List<Message> messages;
    private final Double temperature;
    private final Integer maxTokens;
    private final Double topP;
    private final Double frequencyPenalty;
    private final Double presencePenalty;
    private final List<String> stop;
    private final boolean stream;
    
    private LLMRequest(Builder builder) {
        this.model = Objects.requireNonNull(builder.model, "Model cannot be null");
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.topP = builder.topP;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.presencePenalty = builder.presencePenalty;
        this.stop = builder.stop != null ? Collections.unmodifiableList(new ArrayList<>(builder.stop)) : null;
        this.stream = builder.stream;
        
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty");
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getModel() {
        return model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    public Double getPresencePenalty() {
        return presencePenalty;
    }
    
    public List<String> getStop() {
        return stop;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    /**
     * Builder for constructing LLMRequest instances.
     */
    public static class Builder {
        private String model;
        private List<Message> messages = new ArrayList<>();
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private List<String> stop;
        private boolean stream = false;
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder messages(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
            return this;
        }
        
        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }
        
        public Builder addMessage(String role, String content) {
            this.messages.add(new Message(role, content));
            return this;
        }
        
        public Builder system(String content) {
            return addMessage(Message.system(content));
        }
        
        public Builder user(String content) {
            return addMessage(Message.user(content));
        }
        
        public Builder assistant(String content) {
            return addMessage(Message.assistant(content));
        }
        
        public Builder temperature(double temperature) {
            if (temperature < 0 || temperature > 2) {
                throw new IllegalArgumentException("Temperature must be between 0 and 2");
            }
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            if (maxTokens < 1) {
                throw new IllegalArgumentException("Max tokens must be positive");
            }
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder topP(double topP) {
            if (topP < 0 || topP > 1) {
                throw new IllegalArgumentException("Top P must be between 0 and 1");
            }
            this.topP = topP;
            return this;
        }
        
        public Builder frequencyPenalty(double frequencyPenalty) {
            if (frequencyPenalty < -2 || frequencyPenalty > 2) {
                throw new IllegalArgumentException("Frequency penalty must be between -2 and 2");
            }
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }
        
        public Builder presencePenalty(double presencePenalty) {
            if (presencePenalty < -2 || presencePenalty > 2) {
                throw new IllegalArgumentException("Presence penalty must be between -2 and 2");
            }
            this.presencePenalty = presencePenalty;
            return this;
        }
        
        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }
        
        public Builder stop(String... stop) {
            this.stop = List.of(stop);
            return this;
        }
        
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public LLMRequest build() {
            return new LLMRequest(this);
        }
    }
    
    @Override
    public String toString() {
        return "LLMRequest{model='" + model + "', messages=" + messages.size() + 
               ", temperature=" + temperature + ", maxTokens=" + maxTokens + "}";
    }
}
