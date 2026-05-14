package kr.inventory.domain.chat.service.command;

import kr.inventory.domain.chat.entity.ChatMessage;

public record InterruptedChatResult(
        Long userId,
        Long threadId,
        Long requestMessageId,
        String clientMessageId,
        String reason
) {
    public static InterruptedChatResult from(ChatMessage requestMessage, String reason) {
        return new InterruptedChatResult(
                requestMessage.getThread().getUser().getUserId(),
                requestMessage.getThread().getThreadId(),
                requestMessage.getMessageId(),
                requestMessage.getClientMessageId(),
                reason
        );
    }
}
