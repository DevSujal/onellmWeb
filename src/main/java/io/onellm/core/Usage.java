package io.onellm.core;

/**
 * Token usage information from an LLM response.
 */
public class Usage {
    
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
    
    public Usage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
    
    public Usage(int promptTokens, int completionTokens) {
        this(promptTokens, completionTokens, promptTokens + completionTokens);
    }
    
    public int getPromptTokens() {
        return promptTokens;
    }
    
    public int getCompletionTokens() {
        return completionTokens;
    }
    
    public int getTotalTokens() {
        return totalTokens;
    }
    
    @Override
    public String toString() {
        return "Usage{prompt=" + promptTokens + ", completion=" + completionTokens + 
               ", total=" + totalTokens + "}";
    }
}
