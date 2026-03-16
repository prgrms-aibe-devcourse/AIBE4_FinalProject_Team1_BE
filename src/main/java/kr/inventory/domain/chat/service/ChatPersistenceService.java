package kr.inventory.domain.chat.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import kr.inventory.domain.chat.service.command.QueuedChatDispatchTarget;
import kr.inventory.domain.store.service.StoreAccessValidator;
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
    private final StoreAccessValidator storeAccessValidator;

    public ChatThreadCreateResponse createThread(Long userId, String title, UUID storePublicId) {
        storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        User userReference = entityManager.getReference(User.class, userId);

        ChatThread thread = ChatThread.create(userReference, title, storePublicId);
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

        storeAccessValidator.validateAndGetStoreId(userId, thread.getStorePublicId());

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

    public Optional<QueuedChatDispatchTarget> reserveNextQueuedMessage(Long threadId) {
        boolean processingExists = chatMessageRepository.existsByThreadThreadIdAndRoleAndStatus(
                threadId,
                ChatMessageRole.USER,
                ChatMessageStatus.PROCESSING
        );

        if (processingExists) {
            return Optional.empty();
        }

        ChatMessage nextQueuedMessage = chatMessageRepository
                .findFirstByThreadThreadIdAndRoleAndStatusOrderByMessageIdAsc(
                        threadId,
                        ChatMessageRole.USER,
                        ChatMessageStatus.QUEUED
                )
                .orElse(null);

        if (nextQueuedMessage == null) {
            return Optional.empty();
        }

        nextQueuedMessage.markProcessing();

        return Optional.of(QueuedChatDispatchTarget.from(nextQueuedMessage));
    }

    public void revertToQueued(Long requestMessageId) {
        ChatMessage requestMessage = getRequiredUserMessage(requestMessageId);

        if (requestMessage.getStatus() == ChatMessageStatus.PROCESSING) {
            requestMessage.markQueued();
        }
    }

    public List<Long> findThreadIdsHavingQueuedUserMessages(int limit) {
        return chatMessageRepository.findThreadIdsHavingQueuedUserMessages(limit);
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
