package kr.inventory.domain.chat.service.command;

import kr.inventory.domain.chat.controller.dto.response.ChatMessageResponse;

public record AcceptedUserMessageResult(
        Long userId,
        ChatMessageResponse requestMessage,
        boolean duplicated
) {
}