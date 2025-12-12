package io.onellm.config;

import io.onellm.service.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for OneLLM.
 * 
 * This configuration provides a ProviderFactory bean that creates
 * LLM providers on-demand with user-provided API keys.
 */
@Configuration
public class LLMConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMConfig.class);
    
    @Bean
    public ProviderFactory providerFactory() {
        logger.info("Initializing OneLLM ProviderFactory - users will provide their own API keys");
        return new ProviderFactory();
    }
}
