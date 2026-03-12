package kr.inventory.domain.chat.service.stream;

import kr.inventory.domain.chat.constant.ChatConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamInitializer implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;
    private final ChatStreamProperties chatStreamProperties;

    @Override
    public void run(ApplicationArguments args) {
        Boolean exists = redisTemplate.hasKey(chatStreamProperties.getKey());
        if (exists) {
            return;
        }

        redisTemplate.opsForStream().add(
                chatStreamProperties.getKey(),
                ChatStreamUserMessagePayload.initRecord().toMap()
        );

        try {
            redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.execute(
                            ChatConstants.CMD_XGROUP,
                            b(ChatConstants.XGROUP_CREATE),
                            b(chatStreamProperties.getKey()),
                            b(chatStreamProperties.getConsumerGroup()),
                            b(ChatConstants.XGROUP_START_ID),
                            b(ChatConstants.XGROUP_MKSTREAM)
                    )
            );
            log.info(
                    "chat stream group ensured. key={}, group={}",
                    chatStreamProperties.getKey(),
                    chatStreamProperties.getConsumerGroup()
            );
        } catch (Exception e) {
            log.info("chat stream group already exists(or cannot create): {}", e.getMessage());
        }
    }

    private static byte[] b(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}