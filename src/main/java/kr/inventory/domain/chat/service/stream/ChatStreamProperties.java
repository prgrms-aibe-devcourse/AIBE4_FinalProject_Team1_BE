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

    private long pollCount = 1L;
    private long pollBlockSeconds = 2L;
    private long pollFixedDelayMs = 1000L;

    private int minWorkers = 1;
    private int maxWorkers = 8;
    private long scaleCheckFixedDelayMs = 1000L;
    private long scaleDownIdleSeconds = 15L;

    private long dispatchRecoveryFixedDelayMs = 1000L;
    private int dispatchRecoveryBatchSize = 32;
    private long threadDispatchLockTtlSeconds = 10L;

    private long claimMinIdleSeconds = 30L;
    private long claimCount = 1L;

    private long processingLockTtlSeconds = 120L;
    private long processingLockRenewIntervalSeconds = 20L;
    private long gracefulShutdownWaitSeconds = 30L;
}
