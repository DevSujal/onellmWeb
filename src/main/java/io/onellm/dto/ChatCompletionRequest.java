package io.onellm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for chat completion API.
 */
public class ChatCompletionRequest {
    
    @NotBlank(message = "API key is required")
    private String apiKey;
    
    @NotBlank(message = "Model is required")
    private String model;
    
    @NotEmpty(message = "At least one message is required")
    @Valid
    private List<MessageDTO> messages;
    
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private List<String> stop;
    private Boolean stream;
    
    // Provider-specific optional fields
    private String baseUrl;  // Custom base URL (for OpenAI-compatible endpoints)
    private String azureResourceName;  // Required for Azure
    private String azureDeploymentName;  // Required for Azure
    private String openRouterSiteName;  // Optional for OpenRouter
    private String openRouterSiteUrl;  // Optional for OpenRouter
    
    // Getters and Setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public List<MessageDTO> getMessages() { return messages; }
    public void setMessages(List<MessageDTO> messages) { this.messages = messages; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public void setFrequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
    
    public Double getPresencePenalty() { return presencePenalty; }
    public void setPresencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; }
    
    public List<String> getStop() { return stop; }
    public void setStop(List<String> stop) { this.stop = stop; }
    
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getAzureResourceName() { return azureResourceName; }
    public void setAzureResourceName(String azureResourceName) { this.azureResourceName = azureResourceName; }
    
    public String getAzureDeploymentName() { return azureDeploymentName; }
    public void setAzureDeploymentName(String azureDeploymentName) { this.azureDeploymentName = azureDeploymentName; }
    
    public String getOpenRouterSiteName() { return openRouterSiteName; }
    public void setOpenRouterSiteName(String openRouterSiteName) { this.openRouterSiteName = openRouterSiteName; }
    
    public String getOpenRouterSiteUrl() { return openRouterSiteUrl; }
    public void setOpenRouterSiteUrl(String openRouterSiteUrl) { this.openRouterSiteUrl = openRouterSiteUrl; }
}
