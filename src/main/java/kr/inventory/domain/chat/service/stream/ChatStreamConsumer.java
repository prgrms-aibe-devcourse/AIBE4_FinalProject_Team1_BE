package kr.inventory.domain.chat.service.stream;

import kr.inventory.domain.chat.service.ChatWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ChatStreamProperties chatStreamProperties;
    private final ChatWorkerService chatWorkerService;

    @Scheduled(fixedDelayString = "${chat.stream.poll-fixed-delay-ms:1000}")
    public void poll() {
        List<MapRecord<String, Object, Object>> records;

        try {
            records = redisTemplate.opsForStream().read(
                    Consumer.from(
                            chatStreamProperties.getConsumerGroup(),
                            chatStreamProperties.getConsumerName()
                    ),
                    StreamReadOptions.empty()
                            .count(chatStreamProperties.getPollCount())
                            .block(Duration.ofSeconds(chatStreamProperties.getPollBlockSeconds())),
                    StreamOffset.create(chatStreamProperties.getKey(), ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            log.debug("chat stream read failed: {}", e.getMessage());
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            handleOne(record);
        }
    }

    private void handleOne(MapRecord<String, Object, Object> record) {
        try {
            ChatStreamUserMessagePayload payload = ChatStreamUserMessagePayload.fromRecord(record);

            if (payload.type() == ChatStreamMessageType.INIT) {
                acknowledge(record);
                return;
            }

            chatWorkerService.process(payload);
            acknowledge(record);
        } catch (Exception e) {
            log.error("Failed to handle chat stream record. recordId={}", record.getId(), e);
            acknowledge(record);
        }
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(
                chatStreamProperties.getKey(),
                chatStreamProperties.getConsumerGroup(),
                record.getId()
        );
    }
}