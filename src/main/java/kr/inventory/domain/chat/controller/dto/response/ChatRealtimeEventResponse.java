package kr.inventory.domain.chat.controller.dto.response;

import java.time.OffsetDateTime;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;

public record ChatRealtimeEventResponse(
        ChatRealtimeEventType eventType,
        Long threadId,
        Long requestMessageId,
        String clientMessageId,
        ChatMessageStatus requestStatus,
        ChatMessageResponse message,
        String errorMessage,
        OffsetDateTime occurredAt
) {
}