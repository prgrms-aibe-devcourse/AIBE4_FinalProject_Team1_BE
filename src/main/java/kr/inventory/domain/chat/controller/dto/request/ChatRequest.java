package kr.inventory.domain.chat.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "message는 비어 있을 수 없습니다.")
        String message
) {
}
