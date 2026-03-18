package kr.inventory.global.llm.client.springai;

import kr.inventory.ai.sales.tool.SalesAiTools;
import kr.inventory.ai.stock.tool.StockAiTools;
import kr.inventory.ai.stock.tool.StockShortageAiTools;
import kr.inventory.global.llm.client.LlmClient;
import kr.inventory.global.llm.dto.LlmChatRequest;
import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient inventoryChatClient;
    private final StockAiTools stockAiTools;
    private final SalesAiTools salesAiTools;
    private final StockShortageAiTools stockShortageAiTools;

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        List<Message> promptMessages = new ArrayList<>();

        if (StringUtils.hasText(request.systemPrompt())) {
            promptMessages.add(new SystemMessage(request.systemPrompt().trim()));
        }

        if (request.messages() != null) {
            for (LlmMessage message : request.messages()) {
                if (message == null || !StringUtils.hasText(message.content())) {
                    continue;
                }

                String role = message.role() == null ? "USER" : message.role().trim().toUpperCase();
                String content = message.content().trim();

                switch (role) {
                    case "SYSTEM" -> promptMessages.add(new SystemMessage(content));
                    case "ASSISTANT" -> promptMessages.add(new AssistantMessage(content));
                    case "USER" -> promptMessages.add(new UserMessage(content));
                    default -> promptMessages.add(new UserMessage(content));
                }
            }
        }

        String content = inventoryChatClient
                .prompt(new Prompt(promptMessages))
                .tools(stockAiTools, salesAiTools, stockShortageAiTools)
                .call()
                .content();

        return new LlmChatResponse(content, "google-genai");
    }
}