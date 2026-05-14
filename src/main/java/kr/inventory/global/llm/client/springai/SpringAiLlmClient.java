package kr.inventory.global.llm.client.springai;

import java.util.ArrayList;
import java.util.List;
import kr.inventory.ai.sales.tool.SalesAiTools;
import kr.inventory.ai.stock.tool.StockAiTools;
import kr.inventory.ai.stock.tool.StockInboundAiTools;
import kr.inventory.ai.stock.tool.StockLogAiTools;
import kr.inventory.ai.stock.tool.StockShortageAiTools;
import kr.inventory.global.llm.client.LlmClient;
import kr.inventory.global.llm.dto.LlmChatRequest;
import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.dto.LlmExecutionOptions;
import kr.inventory.global.llm.dto.LlmMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient inventoryChatClient;
    private final SalesAiTools salesAiTools;
    private final StockAiTools stockAiTools;
    private final StockShortageAiTools stockShortageAiTools;
    private final StockInboundAiTools stockInboundAiTools;
    private final StockLogAiTools stockLogAiTools;

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

        var promptSpec = inventoryChatClient.prompt(new Prompt(promptMessages));
        GoogleGenAiChatOptions options = toOptions(request.options());
        if (options != null) {
            promptSpec = promptSpec.options(options);
        }

        if (request.options() == null || request.options().toolEnabled()) {
            promptSpec = promptSpec.tools(salesAiTools, stockAiTools, stockShortageAiTools, stockInboundAiTools, stockLogAiTools);
        }

        String content = promptSpec.call().content();
        String model = request.options() != null && StringUtils.hasText(request.options().model())
                ? request.options().model()
                : "google-genai";

        return new LlmChatResponse(content, model);
    }

    private GoogleGenAiChatOptions toOptions(LlmExecutionOptions options) {
        if (options == null) {
            return null;
        }

        GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder();

        if (StringUtils.hasText(options.model())) {
            builder.model(options.model().trim());
        }
        if (options.temperature() != null) {
            builder.temperature(options.temperature());
        }
        if (options.maxOutputTokens() != null) {
            builder.maxOutputTokens(options.maxOutputTokens());
        }
        if (options.thinkingBudget() != null) {
            builder.thinkingBudget(options.thinkingBudget());
        }

        return builder.build();
    }
}
