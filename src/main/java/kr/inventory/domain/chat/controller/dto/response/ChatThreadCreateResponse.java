package kr.inventory.domain.chat.controller.dto.response;

import java.time.OffsetDateTime;
import kr.inventory.domain.chat.entity.ChatThread;
import kr.inventory.domain.chat.entity.enums.ChatThreadStatus;

public record ChatThreadCreateResponse(
        Long threadId,
        String title,
        ChatThreadStatus status,
        OffsetDateTime createdAt
) {
    public static ChatThreadCreateResponse from(ChatThread thread) {
        return new ChatThreadCreateResponse(
                thread.getThreadId(),
                thread.getTitle(),
                thread.getStatus(),
                thread.getCreatedAt()
        );
    }
}