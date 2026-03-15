package kr.inventory.domain.chat.controller.dto.response;

import java.time.OffsetDateTime;
import kr.inventory.domain.chat.entity.ChatMessage;
import kr.inventory.domain.chat.entity.enums.ChatMessageRole;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;

public record ChatMessageResponse(
        Long messageId,
        Long threadId,
        ChatMessageRole role,
        ChatMessageStatus status,
        String content,
        String clientMessageId,
        Long replyToMessageId,
        String model,
        String errorMessage,
        OffsetDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getMessageId(),
                message.getThread().getThreadId(),
                message.getRole(),
                message.getStatus(),
                message.getContent(),
                message.getClientMessageId(),
                message.getReplyToMessageId(),
                message.getModel(),
                message.getErrorMessage(),
                message.getCreatedAt()
        );
    }
}