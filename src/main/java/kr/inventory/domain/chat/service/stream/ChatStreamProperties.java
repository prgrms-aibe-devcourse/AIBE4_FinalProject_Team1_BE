package kr.inventory.domain.chat.service.stream;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.stream")
public class ChatStreamProperties {

    private String key = "chat:user-message";
    private String consumerGroup = "chat-llm-group";
    private String consumerName = "chat-worker-" + UUID.randomUUID();
    private long pollCount = 10L;
    private long pollBlockSeconds = 2L;
    private long pollFixedDelayMs = 1000L;
}