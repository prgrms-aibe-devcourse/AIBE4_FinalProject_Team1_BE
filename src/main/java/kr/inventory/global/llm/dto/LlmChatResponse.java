package kr.inventory.global.llm.dto;

public record LlmChatResponse(
        String text,
        String model
) {
}