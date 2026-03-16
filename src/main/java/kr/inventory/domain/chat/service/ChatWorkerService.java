package kr.inventory.domain.chat.service;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.entity.ChatThread;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.repository.ChatThreadRepository;
import kr.inventory.domain.chat.service.ChatProcessingLeaseService.ProcessingLease;
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
    private final ChatProcessingLeaseService chatProcessingLeaseService;
    private final ChatThreadDispatchService chatThreadDispatchService;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatToolContextProvider chatToolContextProvider;

    public boolean process(ChatStreamUserMessagePayload payload) {
        ProcessingLease lease = chatProcessingLeaseService.tryAcquire(payload.requestMessageId());
        if (lease == null) {
            log.debug(
                    "Chat request is already being processed by another worker. requestMessageId={}",
                    payload.requestMessageId()
            );
            return false;
        }

        ScheduledFuture<?> heartbeat = chatProcessingLeaseService.startHeartbeat(lease);

        try {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                return false;
            }

            sendProcessingQuietly(
                    payload.userId(),
                    payload.threadId(),
                    payload.requestMessageId(),
                    payload.clientMessageId()
            );

            ChatThread thread = chatThreadRepository.findById(payload.threadId())
                    .orElseThrow(() -> new IllegalStateException("채팅 스레드를 찾을 수 없습니다."));

            chatToolContextProvider.set(
                    new ChatToolContext(
                            payload.userId(),
                            thread.getStorePublicId(),
                            payload.threadId(),
                            payload.requestMessageId()
                    )
            );

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

            sendCompletedQuietly(completed);
            dispatchNextQueuedQuietly(payload.threadId());
            return true;
        } catch (Exception e) {
            if (isWorkerInterrupted(e)) {
                Thread.currentThread().interrupt();
                log.warn(
                        "Chat worker interrupted. requestMessageId={}, threadId={}",
                        payload.requestMessageId(),
                        payload.threadId(),
                        e
                );
                return false;
            }

            log.error(
                    "Failed to process chat message. userId={}, threadId={}, requestMessageId={}",
                    payload.userId(),
                    payload.threadId(),
                    payload.requestMessageId(),
                    e
            );

            try {
                FailedChatResult failed = chatPersistenceService.markFailed(
                        payload.requestMessageId(),
                        truncateError(e.getMessage())
                );
                sendFailedQuietly(failed);
                dispatchNextQueuedQuietly(payload.threadId());
            } catch (Exception persistenceException) {
                log.error(
                        "Failed to persist chat failure state. requestMessageId={}",
                        payload.requestMessageId(),
                        persistenceException
                );
            }

            return true;
        } finally {
            chatToolContextProvider.clear();
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            chatProcessingLeaseService.release(lease);
        }
    }

    private void sendProcessingQuietly(
            Long userId,
            Long threadId,
            Long requestMessageId,
            String clientMessageId
    ) {
        try {
            chatPushService.sendProcessing(userId, threadId, requestMessageId, clientMessageId);
        } catch (Exception e) {
            log.warn(
                    "Failed to push processing event. requestMessageId={}, reason={}",
                    requestMessageId,
                    e.getMessage()
            );
        }
    }

    private void sendCompletedQuietly(CompletedChatResult completed) {
        try {
            chatPushService.sendCompleted(completed);
        } catch (Exception e) {
            log.warn(
                    "Failed to push completed event. requestMessageId={}, reason={}",
                    completed.requestMessageId(),
                    e.getMessage()
            );
        }
    }

    private void sendFailedQuietly(FailedChatResult failed) {
        try {
            chatPushService.sendFailed(failed);
        } catch (Exception e) {
            log.warn(
                    "Failed to push failed event. requestMessageId={}, reason={}",
                    failed.requestMessageId(),
                    e.getMessage()
            );
        }
    }

    private void dispatchNextQueuedQuietly(Long threadId) {
        try {
            chatThreadDispatchService.dispatchHeadOfLine(threadId);
        } catch (Exception e) {
            if (isWorkerInterrupted(e)) {
                Thread.currentThread().interrupt();
                return;
            }

            log.error(
                    "Failed to dispatch next queued chat message. threadId={}",
                    threadId,
                    e
            );
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

    private boolean isWorkerInterrupted(Throwable throwable) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }

        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof InterruptedException
                    || cursor instanceof InterruptedIOException
                    || cursor instanceof ClosedByInterruptException
                    || cursor instanceof CancellationException) {
                return true;
            }
            cursor = cursor.getCause();
        }

        return false;
    }
}
