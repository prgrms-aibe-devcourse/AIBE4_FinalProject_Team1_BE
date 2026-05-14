package kr.inventory.domain.chat.service;

import java.util.UUID;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadCreateResponse;
import kr.inventory.domain.chat.entity.enums.ChatInterruptStrategy;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.exception.ChatException;
import kr.inventory.domain.chat.monitoring.ChatMetricsRecorder;
import kr.inventory.domain.chat.service.command.AcceptedUserMessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final ChatPersistenceService chatPersistenceService;
    private final ChatPushService chatPushService;
    private final ChatThreadDispatchService chatThreadDispatchService;
    private final ChatExecutionRegistry chatExecutionRegistry;
    private final ChatMetricsRecorder chatMetricsRecorder;

    public ChatThreadCreateResponse createThread(Long userId, String rawTitle, UUID storePublicId) {
        String title = normalizeTitle(rawTitle);
        return chatPersistenceService.createThread(userId, title, storePublicId);
    }

    public void acceptUserMessage(
            Long userId,
            Long threadId,
            String rawClientMessageId,
            String rawContent,
            ChatInterruptStrategy rawInterruptStrategy
    ) {
        String clientMessageId = normalizeClientMessageId(rawClientMessageId);
        String content = normalizeContent(rawContent);
        ChatInterruptStrategy interruptStrategy = normalizeInterruptStrategy(rawInterruptStrategy);

        if (interruptStrategy == ChatInterruptStrategy.INTERRUPT_CURRENT
                || (interruptStrategy == ChatInterruptStrategy.AUTO && chatExecutionRegistry.hasActiveExecution(threadId))) {
            requestInterruptQuietly(threadId, "새로운 사용자 질문이 도착하여 현재 답변 생성을 중단합니다.");
        }

        AcceptedUserMessageResult accepted = chatPersistenceService.persistUserMessage(
                userId,
                threadId,
                clientMessageId,
                content
        );

        chatMetricsRecorder.recordAccepted(accepted.duplicated());
        chatPushService.sendAccepted(accepted);
        chatThreadDispatchService.dispatchHeadOfLine(accepted.requestMessage().threadId());
    }

    private void requestInterruptQuietly(Long threadId, String reason) {
        try {
            chatExecutionRegistry.requestInterrupt(threadId, reason)
                    .ifPresent(target -> chatMetricsRecorder.recordInterruptRequested());
        } catch (Exception e) {
            log.warn("Failed to request interrupt. threadId={}, reason={}", threadId, e.getMessage());
        }
    }

    private String normalizeTitle(String rawTitle) {
        if (!StringUtils.hasText(rawTitle)) {
            return ChatConstants.DEFAULT_THREAD_TITLE;
        }
        String title = rawTitle.trim();
        if (title.length() > ChatConstants.MAX_TITLE_LENGTH) {
            throw new ChatException(ChatErrorCode.TITLE_TOO_LONG);
        }
        return title;
    }

    private String normalizeContent(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            throw new ChatException(ChatErrorCode.CONTENT_EMPTY);
        }
        String content = rawContent.trim();
        if (content.length() > ChatConstants.MAX_CONTENT_LENGTH) {
            throw new ChatException(ChatErrorCode.CONTENT_TOO_LONG);
        }
        return content;
    }

    private String normalizeClientMessageId(String rawClientMessageId) {
        if (!StringUtils.hasText(rawClientMessageId)) {
            throw new ChatException(ChatErrorCode.CLIENT_MESSAGE_ID_EMPTY);
        }
        String clientMessageId = rawClientMessageId.trim();
        if (clientMessageId.length() > ChatConstants.MAX_CLIENT_MESSAGE_ID_LENGTH) {
            throw new ChatException(ChatErrorCode.CLIENT_MESSAGE_ID_TOO_LONG);
        }
        return clientMessageId;
    }

    private ChatInterruptStrategy normalizeInterruptStrategy(ChatInterruptStrategy rawInterruptStrategy) {
        return rawInterruptStrategy == null ? ChatInterruptStrategy.AUTO : rawInterruptStrategy;
    }
}
