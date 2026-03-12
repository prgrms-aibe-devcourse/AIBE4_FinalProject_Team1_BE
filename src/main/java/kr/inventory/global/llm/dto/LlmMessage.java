package kr.inventory.global.llm.dto;

public record LlmMessage(
        String role,
        String content
) {
}