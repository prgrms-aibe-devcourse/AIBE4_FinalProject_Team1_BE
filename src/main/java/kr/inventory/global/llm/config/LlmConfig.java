package kr.inventory.global.llm.config;

import kr.inventory.global.llm.client.LlmClient;
import kr.inventory.global.llm.client.gemini.GeminiLlmClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean
    public LlmClient llmClient(
            LlmProperties properties,
            GeminiLlmClient geminiLlmClient
    ) {
        String provider = properties.getProvider();

        if (provider == null) {
            throw new IllegalArgumentException("llm.provider is not configured");
        }

        return switch (provider.toLowerCase()) {
            case "gemini" -> geminiLlmClient;
            default -> throw new IllegalArgumentException("Unsupported llm provider: " + provider);
        };
    }
}