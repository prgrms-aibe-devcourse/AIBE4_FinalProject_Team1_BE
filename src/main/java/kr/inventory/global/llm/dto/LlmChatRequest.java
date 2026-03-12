package kr.inventory.global.llm.dto;

import java.util.List;

public record LlmChatRequest(
        String systemPrompt,
        List<LlmMessage> messages
) {
}