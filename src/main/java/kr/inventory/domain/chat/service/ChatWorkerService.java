package kr.inventory.domain.chat.service;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.entity.ChatThread;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.monitoring.ChatMetricsRecorder;
import kr.inventory.domain.chat.repository.ChatThreadRepository;
import kr.inventory.domain.chat.service.ChatProcessingLeaseService.ProcessingLease;
import kr.inventory.domain.chat.service.command.CompletedChatResult;
import kr.inventory.domain.chat.service.command.FailedChatResult;
import kr.inventory.domain.chat.service.command.InterruptedChatResult;
import kr.inventory.domain.chat.service.context.ChatAnswerPlan;
import kr.inventory.domain.chat.service.context.ChatConversationContext;
import kr.inventory.domain.chat.service.stream.ChatStreamUserMessagePayload;
import kr.inventory.global.llm.dto.LlmChatResponse;
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
    private final ChatAnswerPlanningService chatAnswerPlanningService;
    private final ChatResponseRefinementService chatResponseRefinementService;
    private final LlmService llmService;
    private final ChatPushService chatPushService;
    private final ChatProcessingLeaseService chatProcessingLeaseService;
    private final ChatThreadDispatchService chatThreadDispatchService;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatToolContextProvider chatToolContextProvider;
    private final ChatExecutionRegistry chatExecutionRegistry;
    private final ChatMetricsRecorder chatMetricsRecorder;

    public boolean process(ChatStreamUserMessagePayload payload) {
        long processingStartedNano = System.nanoTime();
        String processingOutcome = "deferred";
        chatMetricsRecorder.recordQueueWait(payload.queuedAtEpochMillis());

        ProcessingLease lease = chatProcessingLeaseService.tryAcquire(payload.requestMessageId());
        if (lease == null) {
            chatMetricsRecorder.recordWorkerSkipped("lease_busy");
            return false;
        }

        ScheduledFuture<?> heartbeat = chatProcessingLeaseService.startHeartbeat(lease);
        boolean registered = false;
        try {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                return false;
            }

            registered = chatExecutionRegistry.register(payload.threadId(), payload.requestMessageId());
            if (!registered) {
                chatMetricsRecorder.recordWorkerSkipped("active_thread_exists");
                return false;
            }

            ensureNotInterrupted(payload.requestMessageId(), "새 요청이 들어와 현재 답변을 시작하기 전에 중단되었습니다.");

            sendProcessingQuietly(payload.userId(), payload.threadId(), payload.requestMessageId(), payload.clientMessageId());

            ChatThread thread = chatThreadRepository.findById(payload.threadId())
                    .orElseThrow(() -> new IllegalStateException("채팅 스레드를 찾을 수 없습니다."));

            chatToolContextProvider.set(new ChatToolContext(
                    payload.userId(),
                    thread.getStorePublicId(),
                    payload.threadId(),
                    payload.requestMessageId()
            ));

            ChatConversationContext conversationContext = chatPromptService.buildConversationContext(
                    payload.threadId(),
                    payload.requestMessageId()
            );
            ChatAnswerPlan answerPlan = chatAnswerPlanningService.plan(conversationContext);

            ensureNotInterrupted(payload.requestMessageId(), "새 요청이 들어와 현재 답변 생성이 중단되었습니다.");

            LlmChatResponse llmResponse = generateAnswer(conversationContext, answerPlan, payload.requestMessageId());

            ensureNotInterrupted(payload.requestMessageId(), "새 요청이 들어와 현재 답변 생성이 중단되었습니다.");

            String assistantContent = normalizeAssistantContent(llmResponse.text());
            assistantContent = normalizeAssistantContent(
                    chatResponseRefinementService.refineIfNeeded(conversationContext, answerPlan, assistantContent)
            );

            ensureNotInterrupted(payload.requestMessageId(), "새 요청이 들어와 현재 답변 생성이 중단되었습니다.");

            CompletedChatResult completed = chatPersistenceService.completeWithAssistantMessage(
                    payload.requestMessageId(),
                    assistantContent,
                    llmResponse.model()
            );

            sendCompletedQuietly(completed);
            dispatchNextQueuedQuietly(payload.threadId());
            processingOutcome = "completed";
            return true;
        } catch (ChatInterruptedException interruptedException) {
            handleInterrupted(payload, interruptedException.getMessage());
            processingOutcome = "interrupted";
            return true;
        } catch (Exception e) {
            if (isWorkerInterrupted(e) && chatExecutionRegistry.isInterruptRequested(payload.requestMessageId())) {
                handleInterrupted(payload, "새 요청 또는 중단 요청으로 현재 답변 생성이 중단되었습니다.");
                processingOutcome = "interrupted";
                return true;
            }
            if (isWorkerInterrupted(e)) {
                Thread.currentThread().interrupt();
                return false;
            }
            log.error("Failed to process chat message. userId={}, threadId={}, requestMessageId={}",
                    payload.userId(), payload.threadId(), payload.requestMessageId(), e);
            try {
                FailedChatResult failed = chatPersistenceService.markFailed(payload.requestMessageId(), truncateError(e.getMessage()));
                sendFailedQuietly(failed);
                dispatchNextQueuedQuietly(payload.threadId());
                processingOutcome = "failed";
            } catch (Exception persistenceException) {
                log.error("Failed to persist chat failure state. requestMessageId={}", payload.requestMessageId(), persistenceException);
            }
            return true;
        } finally {
            chatToolContextProvider.clear();
            if (heartbeat != null) {
                heartbeat.cancel(false);
            }
            if (registered) {
                chatExecutionRegistry.unregister(payload.threadId(), payload.requestMessageId());
            }
            chatProcessingLeaseService.release(lease);
            chatMetricsRecorder.recordProcessingDuration(processingStartedNano, processingOutcome);
        }
    }


    private LlmChatResponse generateAnswer(
            ChatConversationContext conversationContext,
            ChatAnswerPlan answerPlan,
            Long requestMessageId
    ) {
        String systemPrompt = chatPromptService.systemPrompt(answerPlan, conversationContext);

        try {
            long llmStartedNano = System.nanoTime();
            LlmChatResponse response = llmService.chat(
                    systemPrompt,
                    conversationContext.messages(),
                    chatAnswerPlanningService.toExecutionOptions(answerPlan, true)
            );
            chatMetricsRecorder.recordLlmDuration("answer", llmStartedNano);
            return response;
        } catch (Exception e) {
            if (isWorkerInterrupted(e) || !chatAnswerPlanningService.canFallbackToFlash(answerPlan)) {
                throwAsRuntime(e);
            }

            log.warn(
                    "Primary chat generation failed. Retrying with flash fallback. requestMessageId={}, model={}, reason={}",
                    requestMessageId,
                    answerPlan.model(),
                    e.getMessage()
            );
            ensureNotInterrupted(requestMessageId, "새 요청이 들어와 현재 답변 생성이 중단되었습니다.");

            long fallbackStartedNano = System.nanoTime();
            LlmChatResponse fallbackResponse = llmService.chat(
                    systemPrompt,
                    conversationContext.messages(),
                    chatAnswerPlanningService.flashFallbackOptions(true)
            );
            chatMetricsRecorder.recordLlmDuration("answer_fallback", fallbackStartedNano);
            return fallbackResponse;
        }
    }

    private void throwAsRuntime(Exception e) {
        if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(e);
    }

    private void handleInterrupted(ChatStreamUserMessagePayload payload, String reason) {
        try {
            InterruptedChatResult interrupted = chatPersistenceService.markInterrupted(payload.requestMessageId(), reason);
            chatPushService.sendInterrupted(interrupted);
        } catch (Exception e) {
            log.warn("Failed to persist interrupted state. requestMessageId={}, reason={}", payload.requestMessageId(), e.getMessage());
        } finally {
            dispatchNextQueuedQuietly(payload.threadId());
        }
    }

    private void ensureNotInterrupted(Long requestMessageId, String reason) {
        if (Thread.currentThread().isInterrupted() || chatExecutionRegistry.isInterruptRequested(requestMessageId)) {
            throw new ChatInterruptedException(reason);
        }
    }

    private void sendProcessingQuietly(Long userId, Long threadId, Long requestMessageId, String clientMessageId) {
        try {
            chatPushService.sendProcessing(userId, threadId, requestMessageId, clientMessageId);
        } catch (Exception e) {
            log.warn("Failed to push processing event. requestMessageId={}, reason={}", requestMessageId, e.getMessage());
        }
    }

    private void sendCompletedQuietly(CompletedChatResult completed) {
        try {
            chatPushService.sendCompleted(completed);
        } catch (Exception e) {
            log.warn("Failed to push completed event. requestMessageId={}, reason={}", completed.requestMessageId(), e.getMessage());
        }
    }

    private void sendFailedQuietly(FailedChatResult failed) {
        try {
            chatPushService.sendFailed(failed);
        } catch (Exception e) {
            log.warn("Failed to push failed event. requestMessageId={}, reason={}", failed.requestMessageId(), e.getMessage());
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
            log.error("Failed to dispatch next queued chat message. threadId={}", threadId, e);
        }
    }

    private String normalizeAssistantContent(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return ChatErrorCode.ASSISTANT_GENERATION_FAILED.getMessage();
        }
        String content = rawContent.trim();
        return content.length() > ChatConstants.MAX_CONTENT_LENGTH
                ? content.substring(0, ChatConstants.MAX_CONTENT_LENGTH)
                : content;
    }

    private String truncateError(String rawError) {
        if (!StringUtils.hasText(rawError)) {
            return ChatErrorCode.UNKNOWN_ERROR.getMessage();
        }
        String error = rawError.trim();
        return error.length() > ChatConstants.MAX_ERROR_LENGTH
                ? error.substring(0, ChatConstants.MAX_ERROR_LENGTH)
                : error;
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

    private static final class ChatInterruptedException extends RuntimeException {
        private ChatInterruptedException(String message) {
            super(message);
        }
    }
}
