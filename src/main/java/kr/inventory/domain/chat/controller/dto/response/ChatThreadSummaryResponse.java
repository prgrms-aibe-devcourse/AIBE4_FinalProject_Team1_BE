package kr.inventory.domain.chat.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;
import kr.inventory.domain.chat.entity.enums.ChatThreadStatus;

public record ChatThreadSummaryResponse(
        Long threadId,
        UUID storePublicId,
        String title,
        ChatThreadStatus status,
        String lastMessagePreview,
        OffsetDateTime lastMessageAt,
        OffsetDateTime createdAt
) {
}