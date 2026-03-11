package kr.inventory.global.llm.service;

import kr.inventory.global.llm.client.LlmClient;
import kr.inventory.global.llm.dto.LlmChatRequest;
import kr.inventory.global.llm.dto.LlmChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmClient llmClient;

    public LlmChatResponse chat(String systemPrompt, String userPrompt) {
        return llmClient.chat(new LlmChatRequest(systemPrompt, userPrompt));
    }
}