package kr.inventory.domain.chat.service.command;

import kr.inventory.domain.chat.entity.ChatMessage;

public record QueuedChatDispatchTarget(
        Long userId,
        Long threadId,
        Long requestMessageId,
        String clientMessageId,
        String content
) {
    public static QueuedChatDispatchTarget from(ChatMessage requestMessage) {
        return new QueuedChatDispatchTarget(
                requestMessage.getThread().getUser().getUserId(),
                requestMessage.getThread().getThreadId(),
                requestMessage.getMessageId(),
                requestMessage.getClientMessageId(),
                requestMessage.getContent()
        );
    }
}
