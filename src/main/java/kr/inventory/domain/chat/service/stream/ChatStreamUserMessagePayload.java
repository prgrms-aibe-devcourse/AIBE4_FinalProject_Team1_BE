package kr.inventory.domain.chat.service.stream;

import java.util.LinkedHashMap;
import java.util.Map;
import kr.inventory.domain.chat.constant.ChatConstants;
import org.springframework.data.redis.connection.stream.MapRecord;

public record ChatStreamUserMessagePayload(
        ChatStreamMessageType type,
        Long userId,
        Long threadId,
        Long requestMessageId,
        String clientMessageId,
        String content
) {
    public static ChatStreamUserMessagePayload initRecord() {
        return new ChatStreamUserMessagePayload(
                ChatStreamMessageType.INIT,
                null,
                null,
                null,
                null,
                "INIT"
        );
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ChatConstants.TYPE, type.name());

        if (userId != null) {
            map.put(ChatConstants.USER_ID, String.valueOf(userId));
        }
        if (threadId != null) {
            map.put(ChatConstants.THREAD_ID, String.valueOf(threadId));
        }
        if (requestMessageId != null) {
            map.put(ChatConstants.REQUEST_MESSAGE_ID, String.valueOf(requestMessageId));
        }
        if (clientMessageId != null) {
            map.put(ChatConstants.CLIENT_MESSAGE_ID, clientMessageId);
        }
        if (content != null) {
            map.put(ChatConstants.CONTENT, content);
        }

        return map;
    }

    public static ChatStreamUserMessagePayload fromRecord(MapRecord<String, Object, Object> record) {
        return fromMap(record.getValue());
    }

    public static ChatStreamUserMessagePayload fromMap(Map<?, ?> value) {
        ChatStreamMessageType type = ChatStreamMessageType.valueOf(String.valueOf(value.get(ChatConstants.TYPE)));

        Long userId = value.containsKey(ChatConstants.USER_ID)
                ? Long.parseLong(String.valueOf(value.get(ChatConstants.USER_ID)))
                : null;

        Long threadId = value.containsKey(ChatConstants.THREAD_ID)
                ? Long.parseLong(String.valueOf(value.get(ChatConstants.THREAD_ID)))
                : null;

        Long requestMessageId = value.containsKey(ChatConstants.REQUEST_MESSAGE_ID)
                ? Long.parseLong(String.valueOf(value.get(ChatConstants.REQUEST_MESSAGE_ID)))
                : null;

        String clientMessageId = value.containsKey(ChatConstants.CLIENT_MESSAGE_ID)
                ? String.valueOf(value.get(ChatConstants.CLIENT_MESSAGE_ID))
                : null;

        String content = value.containsKey(ChatConstants.CONTENT)
                ? String.valueOf(value.get(ChatConstants.CONTENT))
                : null;

        return new ChatStreamUserMessagePayload(
                type,
                userId,
                threadId,
                requestMessageId,
                clientMessageId,
                content
        );
    }
}