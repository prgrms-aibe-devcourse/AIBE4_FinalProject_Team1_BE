package kr.inventory.domain.chat.service;

import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmService llmService;

    public String answer(Long userId, String question) {
        String systemPrompt = """
                사용자의 질문에 한국어로 답변해.
                """;

        LlmChatResponse response = llmService.chat(systemPrompt, question);
        return response.text();
    }
}