package kr.inventory.domain.chat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.entity.ChatMessage;
import kr.inventory.domain.chat.repository.ChatMessageRepository;
import kr.inventory.global.llm.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatPromptService {

    private final ChatMessageRepository chatMessageRepository;

    public String systemPrompt() {
        return """
            You are the internal assistant for this service.
            Always respond in Korean.
            Provide precise, concrete, and unambiguous answers.
            Use relevant conversation history when it helps answer the current request.
            Do not invent facts, make unsupported assumptions, or present uncertain information as certain.
            If necessary information is missing, say so clearly and explain what can be concluded from the available context.
            If the user's request is ambiguous, briefly identify the ambiguity and answer based on the most well-supported interpretation.
            Prefer actionable and practically useful guidance over vague summaries.
            Keep the response concise, but do not omit important conditions, edge cases, or limitations.
            When helpful, clearly distinguish confirmed facts, assumptions, and recommendations.
            """;
    }

    public List<LlmMessage> buildConversationMessages(Long threadId, Long upToMessageId) {
        List<ChatMessage> messages = new ArrayList<>(
                chatMessageRepository.findPromptMessages(threadId, upToMessageId, ChatConstants.DEFAULT_CONTEXT_SIZE)
        );

        Collections.reverse(messages);

        return messages.stream()
                .map(this::toLlmMessage)
                .toList();
    }

    private LlmMessage toLlmMessage(ChatMessage message) {
        String role = switch (message.getRole()) {
            case USER -> "USER";
            case ASSISTANT -> "ASSISTANT";
            case SYSTEM -> "SYSTEM";
            case TOOL -> "TOOL";
        };

        return new LlmMessage(role, message.getContent());
    }
}