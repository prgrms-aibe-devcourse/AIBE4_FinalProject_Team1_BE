package kr.inventory.global.llm.dto;

public record LlmChatRequest(
        String systemPrompt,
        String userPrompt
) {
}
