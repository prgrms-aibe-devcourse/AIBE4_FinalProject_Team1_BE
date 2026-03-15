package kr.inventory.domain.chat.service.command;

import kr.inventory.domain.chat.entity.ChatMessage;

public record FailedChatResult(
        Long userId,
        Long threadId,
        Long requestMessageId,
        String clientMessageId,
        String errorMessage
) {
    public static FailedChatResult from(ChatMessage requestMessage, String errorMessage) {
        return new FailedChatResult(
                requestMessage.getThread().getUser().getUserId(),
                requestMessage.getThread().getThreadId(),
                requestMessage.getMessageId(),
                requestMessage.getClientMessageId(),
                errorMessage
        );
    }
}