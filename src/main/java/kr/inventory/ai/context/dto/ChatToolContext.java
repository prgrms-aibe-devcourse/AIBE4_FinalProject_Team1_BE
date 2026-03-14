package kr.inventory.ai.context.dto;

import java.util.UUID;

public record ChatToolContext(
        Long userId,
        UUID storePublicId,
        Long threadId,
        Long requestMessageId
) {
}
