package kr.inventory.domain.chat.service;

import jakarta.persistence.EntityManager;
import kr.inventory.domain.chat.controller.dto.response.ChatMessageResponse;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadCreateResponse;
import kr.inventory.domain.chat.entity.ChatMessage;
import kr.inventory.domain.chat.entity.ChatThread;
import kr.inventory.domain.chat.entity.enums.ChatMessageRole;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.exception.ChatException;
import kr.inventory.domain.chat.repository.ChatMessageRepository;
import kr.inventory.domain.chat.repository.ChatThreadRepository;
import kr.inventory.domain.chat.service.command.AcceptedUserMessageResult;
import kr.inventory.domain.chat.service.command.CompletedChatResult;
import kr.inventory.domain.chat.service.command.FailedChatResult;
import kr.inventory.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatPersistenceService {

    private final EntityManager entityManager;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatThreadCreateResponse createThread(Long userId, String title) {
        User userReference = entityManager.getReference(User.class, userId);

        ChatThread thread = ChatThread.create(userReference, title);
        chatThreadRepository.saveAndFlush(thread);

        return ChatThreadCreateResponse.from(thread);
    }

    public AcceptedUserMessageResult persistUserMessage(
            Long userId,
            Long threadId,
            String clientMessageId,
            String content
    ) {
        User userReference = entityManager.getReference(User.class, userId);

        ChatThread thread = chatThreadRepository.findActiveThreadByIdAndUser(threadId, userReference)
                .orElseThrow(() -> new ChatException(ChatErrorCode.THREAD_NOT_FOUND));

        ChatMessage duplicated = chatMessageRepository.findByThreadThreadIdAndClientMessageId(threadId, clientMessageId)
                .orElse(null);

        if (duplicated != null) {
            return new AcceptedUserMessageResult(
                    userId,
                    ChatMessageResponse.from(duplicated),
                    true
            );
        }

        ChatMessage userMessage = ChatMessage.createUserMessage(thread, content, clientMessageId);
        chatMessageRepository.saveAndFlush(userMessage);

        thread.touchLastMessageAt(userMessage.getCreatedAt());

        return new AcceptedUserMessageResult(
                userId,
                ChatMessageResponse.from(userMessage),
                false
        );
    }

    public void markProcessing(Long requestMessageId) {
        ChatMessage requestMessage = getRequiredUserMessage(requestMessageId);

        if (requestMessage.getStatus() == ChatMessageStatus.QUEUED) {
            requestMessage.markProcessing();
        }
    }

    public CompletedChatResult completeWithAssistantMessage(
            Long requestMessageId,
            String assistantContent,
            String model
    ) {
        ChatMessage requestMessage = getRequiredUserMessage(requestMessageId);
        Long userId = requestMessage.getThread().getUser().getUserId();

        if (requestMessage.getStatus() == ChatMessageStatus.COMPLETED) {
            ChatMessage existingAssistant = chatMessageRepository
                    .findFirstByReplyToMessageIdOrderByMessageIdAsc(requestMessageId)
                    .orElseThrow(() -> new ChatException(ChatErrorCode.ALREADY_COMPLETED_WITHOUT_RESPONSE));

            return new CompletedChatResult(
                    userId,
                    requestMessageId,
                    ChatMessageResponse.from(existingAssistant)
            );
        }

        if (requestMessage.getStatus() == ChatMessageStatus.FAILED) {
            throw new ChatException(ChatErrorCode.ALREADY_FAILED);
        }

        ChatThread thread = requestMessage.getThread();

        ChatMessage assistantMessage = ChatMessage.createAssistantMessage(
                thread,
                assistantContent,
                requestMessageId,
                model
        );
        chatMessageRepository.saveAndFlush(assistantMessage);

        requestMessage.markCompleted();
        thread.touchLastMessageAt(assistantMessage.getCreatedAt());

        return new CompletedChatResult(
                userId,
                requestMessageId,
                ChatMessageResponse.from(assistantMessage)
        );
    }

    public FailedChatResult markQueuePublishFailed(Long requestMessageId, String errorMessage) {
        ChatMessage requestMessage = getRequiredUserMessage(requestMessageId);
        requestMessage.markFailed(errorMessage);

        return FailedChatResult.from(requestMessage, errorMessage);
    }

    public FailedChatResult markFailed(Long requestMessageId, String errorMessage) {
        ChatMessage requestMessage = getRequiredUserMessage(requestMessageId);

        if (requestMessage.getStatus() != ChatMessageStatus.COMPLETED) {
            requestMessage.markFailed(errorMessage);
        }

        return FailedChatResult.from(requestMessage, errorMessage);
    }

    private ChatMessage getRequiredUserMessage(Long requestMessageId) {
        ChatMessage requestMessage = chatMessageRepository.findById(requestMessageId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));

        if (requestMessage.getRole() != ChatMessageRole.USER) {
            throw new ChatException(ChatErrorCode.NOT_USER_MESSAGE);
        }

        return requestMessage;
    }
}