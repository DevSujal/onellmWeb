package io.onellm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OneLLM Spring Boot Application.
 * Provides REST API access to multiple LLM providers through a unified interface.
 */
@SpringBootApplication
public class OneLLMApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OneLLMApplication.class, args);
    }
}
