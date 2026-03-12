package kr.inventory.global.llm.client;

import kr.inventory.global.llm.dto.LlmChatRequest;
import kr.inventory.global.llm.dto.LlmChatResponse;

public interface LlmClient {
    LlmChatResponse chat(LlmChatRequest request);
}