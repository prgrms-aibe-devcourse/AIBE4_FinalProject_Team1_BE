package kr.inventory.domain.chat.service.command;

import kr.inventory.domain.chat.controller.dto.response.ChatMessageResponse;

public record CompletedChatResult(
        Long userId,
        Long requestMessageId,
        ChatMessageResponse assistantMessage
) {
}