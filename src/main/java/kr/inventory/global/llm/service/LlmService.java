package kr.inventory.global.llm.service;

import kr.inventory.global.llm.client.LlmClient;
import kr.inventory.global.llm.dto.LlmChatRequest;
import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmClient llmClient;

    public LlmChatResponse chat(String systemPrompt, String userPrompt) {
        return llmClient.chat(
                new LlmChatRequest(
                        systemPrompt,
                        List.of(new LlmMessage("USER", userPrompt))
                )
        );
    }

    public LlmChatResponse chat(String systemPrompt, List<LlmMessage> messages) {
        return llmClient.chat(new LlmChatRequest(systemPrompt, messages));
    }
}
