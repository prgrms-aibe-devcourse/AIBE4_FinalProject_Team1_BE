package kr.inventory.domain.chat.service.context;

import java.util.List;
import kr.inventory.global.llm.dto.LlmMessage;

public record ChatConversationContext(
        List<LlmMessage> messages,
        String currentUserMessage,
        String previousUserMessage,
        String previousAssistantMessage,
        boolean followUpQuestion,
        String relatedContextBlock
) {
}
