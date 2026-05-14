package kr.inventory.domain.chat.service.stream;

import kr.inventory.domain.chat.monitoring.ChatMetricsRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatStreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatStreamProperties chatStreamProperties;
    private final ChatMetricsRecorder chatMetricsRecorder;

    public void publishUserMessage(ChatStreamUserMessagePayload payload) {
        MapRecord<String, String, String> record = StreamRecords
                .mapBacked(payload.toMap())
                .withStreamKey(chatStreamProperties.getKey());

        stringRedisTemplate.opsForStream().add(record);
        chatMetricsRecorder.recordStreamPublished();
    }
}
