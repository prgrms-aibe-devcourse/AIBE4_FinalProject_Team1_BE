package kr.inventory.domain.chat.service;

import java.util.List;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.service.command.CompletedChatResult;
import kr.inventory.domain.chat.service.command.FailedChatResult;
import kr.inventory.domain.chat.service.stream.ChatStreamUserMessagePayload;
import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.dto.LlmMessage;
import kr.inventory.global.llm.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWorkerService {

    private final ChatPersistenceService chatPersistenceService;
    private final ChatPromptService chatPromptService;
    private final LlmService llmService;
    private final ChatPushService chatPushService;

    public void process(ChatStreamUserMessagePayload payload) {
        chatPersistenceService.markProcessing(payload.requestMessageId());

        chatPushService.sendProcessing(
                payload.userId(),
                payload.threadId(),
                payload.requestMessageId(),
                payload.clientMessageId()
        );

        try {
            List<LlmMessage> messages = chatPromptService.buildConversationMessages(
                    payload.threadId(),
                    payload.requestMessageId()
            );

            LlmChatResponse llmResponse = llmService.chat(
                    chatPromptService.systemPrompt(),
                    messages
            );

            String assistantContent = normalizeAssistantContent(llmResponse.text());

            CompletedChatResult completed = chatPersistenceService.completeWithAssistantMessage(
                    payload.requestMessageId(),
                    assistantContent,
                    llmResponse.model()
            );

            chatPushService.sendCompleted(completed);
        } catch (Exception e) {
            log.error(
                    "Failed to process chat message. userId={}, threadId={}, requestMessageId={}",
                    payload.userId(),
                    payload.threadId(),
                    payload.requestMessageId(),
                    e
            );

            FailedChatResult failed = chatPersistenceService.markFailed(
                    payload.requestMessageId(),
                    truncateError(e.getMessage())
            );

            chatPushService.sendFailed(failed);
        }
    }

    private String normalizeAssistantContent(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return ChatErrorCode.ASSISTANT_GENERATION_FAILED.getMessage();
        }

        String content = rawContent.trim();
        if (content.length() > ChatConstants.MAX_CONTENT_LENGTH) {
            return content.substring(0, ChatConstants.MAX_CONTENT_LENGTH);
        }

        return content;
    }

    private String truncateError(String rawError) {
        if (!StringUtils.hasText(rawError)) {
            return ChatErrorCode.UNKNOWN_ERROR.getMessage();
        }

        String error = rawError.trim();
        if (error.length() > ChatConstants.MAX_ERROR_LENGTH) {
            return error.substring(0, ChatConstants.MAX_ERROR_LENGTH);
        }

        return error;
    }
}