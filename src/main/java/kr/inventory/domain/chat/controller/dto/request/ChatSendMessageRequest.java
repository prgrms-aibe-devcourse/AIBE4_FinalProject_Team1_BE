package kr.inventory.domain.chat.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.inventory.domain.chat.entity.enums.ChatInterruptStrategy;

public record ChatSendMessageRequest(
        @NotNull(message = "threadId는 필수입니다.")
        Long threadId,

        @NotBlank(message = "clientMessageId는 필수입니다.")
        @Size(max = 100, message = "clientMessageId는 100자를 초과할 수 없습니다.")
        String clientMessageId,

        @NotBlank(message = "content는 비어 있을 수 없습니다.")
        @Size(max = 4000, message = "content는 4000자를 초과할 수 없습니다.")
        String content,

        ChatInterruptStrategy interruptStrategy
) {
}
