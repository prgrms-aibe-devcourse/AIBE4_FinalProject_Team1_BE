package kr.inventory.domain.chat.service;

import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadCreateResponse;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.exception.ChatException;
import kr.inventory.domain.chat.service.command.AcceptedUserMessageResult;
import kr.inventory.domain.chat.service.command.FailedChatResult;
import kr.inventory.domain.chat.service.stream.ChatStreamMessageType;
import kr.inventory.domain.chat.service.stream.ChatStreamPublisher;
import kr.inventory.domain.chat.service.stream.ChatStreamUserMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final ChatPersistenceService chatPersistenceService;
    private final ChatStreamPublisher chatStreamPublisher;
    private final ChatPushService chatPushService;

    public ChatThreadCreateResponse createThread(Long userId, String rawTitle, UUID storePublicId) {
        String title = normalizeTitle(rawTitle);
        return chatPersistenceService.createThread(userId, title, storePublicId);
    }

    public void acceptUserMessage(
            Long userId,
            Long threadId,
            String rawClientMessageId,
            String rawContent
    ) {
        String clientMessageId = normalizeClientMessageId(rawClientMessageId);
        String content = normalizeContent(rawContent);

        AcceptedUserMessageResult accepted = chatPersistenceService.persistUserMessage(
                userId,
                threadId,
                clientMessageId,
                content
        );

        if (accepted.duplicated()) {
            chatPushService.sendAccepted(accepted);
            return;
        }

        try {
            chatStreamPublisher.publishUserMessage(
                    new ChatStreamUserMessagePayload(
                            ChatStreamMessageType.USER_MESSAGE,
                            accepted.userId(),
                            accepted.requestMessage().threadId(),
                            accepted.requestMessage().messageId(),
                            accepted.requestMessage().clientMessageId(),
                            accepted.requestMessage().content()
                    )
            );

            chatPushService.sendAccepted(accepted);
        } catch (Exception e) {
            log.error(
                    "Failed to publish chat user message to Redis Stream. userId={}, threadId={}, requestMessageId={}",
                    accepted.userId(),
                    accepted.requestMessage().threadId(),
                    accepted.requestMessage().messageId(),
                    e
            );

            FailedChatResult failed = chatPersistenceService.markQueuePublishFailed(
                    accepted.requestMessage().messageId(),
                    truncateError(e.getMessage())
            );

            chatPushService.sendFailed(failed);
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