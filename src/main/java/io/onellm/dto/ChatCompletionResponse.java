package io.onellm.dto;

import io.onellm.core.LLMResponse;
import io.onellm.core.Usage;

/**
 * Response DTO for chat completion API.
 */
public class ChatCompletionResponse {
    
    private String id;
    private String model;
    private String content;
    private String finishReason;
    private UsageDTO usage;
    private String provider;
    private long latencyMs;
    
    public ChatCompletionResponse() {}
    
    public static ChatCompletionResponse fromLLMResponse(LLMResponse response) {
        ChatCompletionResponse dto = new ChatCompletionResponse();
        dto.setId(response.getId());
        dto.setModel(response.getModel());
        dto.setContent(response.getContent());
        dto.setFinishReason(response.getFinishReason());
        dto.setProvider(response.getProvider());
        dto.setLatencyMs(response.getLatencyMs());
        
        if (response.getUsage() != null) {
            dto.setUsage(UsageDTO.fromUsage(response.getUsage()));
        }
        
        return dto;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    
    public UsageDTO getUsage() { return usage; }
    public void setUsage(UsageDTO usage) { this.usage = usage; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    
    /**
     * Nested DTO for token usage.
     */
    public static class UsageDTO {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        
        public static UsageDTO fromUsage(Usage usage) {
            UsageDTO dto = new UsageDTO();
            dto.setPromptTokens(usage.getPromptTokens());
            dto.setCompletionTokens(usage.getCompletionTokens());
            dto.setTotalTokens(usage.getTotalTokens());
            return dto;
        }
        
        public int getPromptTokens() { return promptTokens; }
        public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
        
        public int getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
        
        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    }
}
