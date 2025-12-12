package io.onellm;

import io.onellm.core.*;
import io.onellm.exception.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OneLLM SDK.
 */
public class OneLLMTest {
    
    @Test
    void testBuilderRequiresProvider() {
        assertThrows(IllegalStateException.class, () -> {
            OneLLM.builder().build();
        });
    }
    
    @Test
    void testLLMRequestBuilder() {
        LLMRequest request = LLMRequest.builder()
                .model("gpt-4")
                .system("You are a helpful assistant")
                .user("Hello")
                .temperature(0.7)
                .maxTokens(100)
                .build();
        
        assertEquals("gpt-4", request.getModel());
        assertEquals(2, request.getMessages().size());
        assertEquals("system", request.getMessages().get(0).getRole());
        assertEquals("user", request.getMessages().get(1).getRole());
        assertEquals(0.7, request.getTemperature(), 0.001);
        assertEquals(100, request.getMaxTokens());
    }
    
    @Test
    void testRequestRequiresModel() {
        assertThrows(NullPointerException.class, () -> {
            LLMRequest.builder()
                    .user("Hello")
                    .build();
        });
    }
    
    @Test
    void testRequestRequiresMessages() {
        assertThrows(IllegalArgumentException.class, () -> {
            LLMRequest.builder()
                    .model("gpt-4")
                    .build();
        });
    }
    
    @Test
    void testTemperatureValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            LLMRequest.builder()
                    .model("gpt-4")
                    .user("Hello")
                    .temperature(3.0)  // Invalid - must be 0-2
                    .build();
        });
    }
    
    @Test
    void testMessageCreation() {
        Message userMsg = Message.user("Hello");
        Message systemMsg = Message.system("You are helpful");
        Message assistantMsg = Message.assistant("Hi there!");
        
        assertEquals("user", userMsg.getRole());
        assertEquals("Hello", userMsg.getContent());
        assertEquals("system", systemMsg.getRole());
        assertEquals("assistant", assistantMsg.getRole());
    }
    
    @Test
    void testUsageCalculation() {
        Usage usage = new Usage(100, 50);
        assertEquals(100, usage.getPromptTokens());
        assertEquals(50, usage.getCompletionTokens());
        assertEquals(150, usage.getTotalTokens());
    }
    
    @Test
    void testLLMResponseBuilder() {
        LLMResponse response = LLMResponse.builder()
                .id("resp-123")
                .model("gpt-4")
                .content("Hello World!")
                .finishReason("stop")
                .usage(100, 20)
                .provider("openai")
                .latencyMs(500)
                .build();
        
        assertEquals("resp-123", response.getId());
        assertEquals("gpt-4", response.getModel());
        assertEquals("Hello World!", response.getContent());
        assertEquals("stop", response.getFinishReason());
        assertEquals(120, response.getUsage().getTotalTokens());
        assertEquals("openai", response.getProvider());
        assertEquals(500, response.getLatencyMs());
    }
    
    @Test
    void testLLMExceptionTypes() {
        LLMException rateLimitError = new LLMException("openai", "Rate limit", 429);
        assertTrue(rateLimitError.isRateLimitError());
        assertFalse(rateLimitError.isAuthenticationError());
        
        LLMException authError = new LLMException("anthropic", "Unauthorized", 401);
        assertTrue(authError.isAuthenticationError());
        assertFalse(authError.isRateLimitError());
        
        LLMException serverError = new LLMException("google", "Server error", 500);
        assertTrue(serverError.isServerError());
        
        ModelNotFoundException modelError = new ModelNotFoundException("unknown-model");
        assertEquals("unknown-model", modelError.getModel());
    }
}
