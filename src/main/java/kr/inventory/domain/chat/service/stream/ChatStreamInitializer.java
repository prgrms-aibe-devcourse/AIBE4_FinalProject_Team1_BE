package kr.inventory.domain.chat.service.stream;

import java.nio.charset.StandardCharsets;
import kr.inventory.global.constant.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamInitializer implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;
    private final ChatStreamProperties chatStreamProperties;

    @Override
    public void run(ApplicationArguments args) {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.execute(
                            WebSocketConstants.CMD_XGROUP,
                            b(WebSocketConstants.XGROUP_CREATE),
                            b(chatStreamProperties.getKey()),
                            b(chatStreamProperties.getConsumerGroup()),
                            b(WebSocketConstants.XGROUP_START_ID),
                            b(WebSocketConstants.XGROUP_MKSTREAM)
                    )
            );

            log.info(
                    "chat stream group created. key={}, group={}",
                    chatStreamProperties.getKey(),
                    chatStreamProperties.getConsumerGroup()
            );
        } catch (Exception e) {
            log.info(
                    "chat stream group ensured. key={}, group={}, reason={}",
                    chatStreamProperties.getKey(),
                    chatStreamProperties.getConsumerGroup(),
                    e.getMessage()
            );
        }
    }

    private static byte[] b(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}