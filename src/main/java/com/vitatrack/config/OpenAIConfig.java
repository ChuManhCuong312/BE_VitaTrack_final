package com.vitatrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI configuration holder.
 * WebClient bean is built dynamically inside OpenAIService.
 * Add additional OpenAI configuration here as needed (e.g. rate limiting, retry).
 */
@Configuration
public class OpenAIConfig {

    @Value("${openai.api-key}")
    public String apiKey;

    @Value("${openai.base-url}")
    public String baseUrl;

    @Value("${openai.model}")
    public String model;

    @Value("${openai.vision-model}")
    public String visionModel;

    @Value("${openai.max-tokens}")
    public int maxTokens;
}
