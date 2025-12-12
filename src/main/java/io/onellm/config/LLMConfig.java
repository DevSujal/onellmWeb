package io.onellm.config;

import io.onellm.OneLLM;
import io.onellm.providers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for OneLLM.
 * Creates and configures the OneLLM instance based on application properties.
 */
@Configuration
@EnableConfigurationProperties(LLMProperties.class)
public class LLMConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMConfig.class);
    
    @Bean
    public OneLLM oneLLM(LLMProperties properties) {
        OneLLM.Builder builder = OneLLM.builder();
        int configuredCount = 0;
        
        // Configure OpenAI
        if (properties.getOpenai().isEnabled()) {
            if (properties.getOpenai().getBaseUrl() != null) {
                builder.openai(properties.getOpenai().getApiKey(), properties.getOpenai().getBaseUrl());
            } else {
                builder.openai(properties.getOpenai().getApiKey());
            }
            logger.info("Configured OpenAI provider");
            configuredCount++;
        }
        
        // Configure Anthropic
        if (properties.getAnthropic().isEnabled()) {
            builder.anthropic(properties.getAnthropic().getApiKey());
            logger.info("Configured Anthropic provider");
            configuredCount++;
        }
        
        // Configure Google
        if (properties.getGoogle().isEnabled()) {
            builder.google(properties.getGoogle().getApiKey());
            logger.info("Configured Google provider");
            configuredCount++;
        }
        
        // Configure Azure
        if (properties.getAzure().isEnabled()) {
            builder.azure(
                properties.getAzure().getApiKey(),
                properties.getAzure().getResourceName(),
                properties.getAzure().getDeploymentName()
            );
            logger.info("Configured Azure OpenAI provider");
            configuredCount++;
        }
        
        // Configure Groq
        if (properties.getGroq().isEnabled()) {
            builder.groq(properties.getGroq().getApiKey());
            logger.info("Configured Groq provider");
            configuredCount++;
        }
        
        // Configure Cerebras
        if (properties.getCerebras().isEnabled()) {
            builder.cerebras(properties.getCerebras().getApiKey());
            logger.info("Configured Cerebras provider");
            configuredCount++;
        }
        
        // Configure Ollama (enabled by default)
        if (properties.getOllama().isEnabled()) {
            builder.ollama(properties.getOllama().getBaseUrl());
            logger.info("Configured Ollama provider at {}", properties.getOllama().getBaseUrl());
            configuredCount++;
        }
        
        // Configure OpenRouter
        if (properties.getOpenrouter().isEnabled()) {
            if (properties.getOpenrouter().getSiteName() != null) {
                builder.openRouter(
                    properties.getOpenrouter().getApiKey(),
                    properties.getOpenrouter().getSiteName(),
                    properties.getOpenrouter().getSiteUrl()
                );
            } else {
                builder.openRouter(properties.getOpenrouter().getApiKey());
            }
            logger.info("Configured OpenRouter provider");
            configuredCount++;
        }
        
        // Configure xAI
        if (properties.getXai().isEnabled()) {
            builder.xai(properties.getXai().getApiKey());
            logger.info("Configured xAI provider");
            configuredCount++;
        }
        
        // Configure Copilot
        if (properties.getCopilot().isEnabled()) {
            builder.copilot(properties.getCopilot().getApiKey());
            logger.info("Configured Copilot provider");
            configuredCount++;
        }
        
        if (configuredCount == 0) {
            logger.warn("No LLM providers configured! Add API keys to application.properties");
            // Add Ollama as fallback for local development
            builder.ollama();
            logger.info("Added Ollama as fallback provider");
        }
        
        return builder.build();
    }
}
