package io.onellm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request DTO for chat completion API.
 */
public class ChatCompletionRequest {
    
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
}
