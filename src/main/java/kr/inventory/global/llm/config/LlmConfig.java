package kr.inventory.global.llm.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {
    @Bean
    public ChatClient inventoryChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}