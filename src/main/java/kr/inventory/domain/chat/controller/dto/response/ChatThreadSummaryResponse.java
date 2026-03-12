package kr.inventory.domain.chat.controller.dto.response;

import java.time.OffsetDateTime;
import kr.inventory.domain.chat.entity.enums.ChatThreadStatus;

public record ChatThreadSummaryResponse(
        Long threadId,
        String title,
        ChatThreadStatus status,
        String lastMessagePreview,
        OffsetDateTime lastMessageAt,
        OffsetDateTime createdAt
) {
}