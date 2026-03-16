package kr.inventory.domain.chat.service;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.List;
import java.util.concurrent.CancellationException;
import kr.inventory.domain.chat.service.ChatProcessingLeaseService.ThreadDispatchLease;
import kr.inventory.domain.chat.service.command.QueuedChatDispatchTarget;
import kr.inventory.domain.chat.service.stream.ChatStreamMessageType;
import kr.inventory.domain.chat.service.stream.ChatStreamProperties;
import kr.inventory.domain.chat.service.stream.ChatStreamPublisher;
import kr.inventory.domain.chat.service.stream.ChatStreamUserMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatThreadDispatchService {

    private final ChatPersistenceService chatPersistenceService;
    private final ChatStreamPublisher chatStreamPublisher;
    private final ChatProcessingLeaseService chatProcessingLeaseService;
    private final ChatStreamProperties chatStreamProperties;

    public void dispatchHeadOfLine(Long threadId) {
        if (threadId == null) {
            return;
        }

        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            return;
        }

        ThreadDispatchLease dispatchLease = chatProcessingLeaseService.tryAcquireThreadDispatchLease(threadId);
        if (dispatchLease == null) {
            return;
        }

        QueuedChatDispatchTarget reservedTarget = null;

        try {
            reservedTarget = chatPersistenceService.reserveNextQueuedMessage(threadId).orElse(null);
            if (reservedTarget == null) {
                return;
            }

            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                revertToQueuedQuietly(reservedTarget.requestMessageId());
                return;
            }

            chatStreamPublisher.publishUserMessage(
                    new ChatStreamUserMessagePayload(
                            ChatStreamMessageType.USER_MESSAGE,
                            reservedTarget.userId(),
                            reservedTarget.threadId(),
                            reservedTarget.requestMessageId(),
                            reservedTarget.clientMessageId(),
                            reservedTarget.content()
                    )
            );
        } catch (Exception e) {
            if (reservedTarget != null) {
                revertToQueuedQuietly(reservedTarget.requestMessageId());
            }

            if (isInterruptLike(e)) {
                Thread.currentThread().interrupt();
                log.warn(
                        "Interrupted while dispatching chat head-of-line. threadId={}, requestMessageId={}",
                        threadId,
                        reservedTarget != null ? reservedTarget.requestMessageId() : null,
                        e
                );
                return;
            }

            log.error(
                    "Failed to dispatch chat head-of-line. threadId={}, requestMessageId={}",
                    threadId,
                    reservedTarget != null ? reservedTarget.requestMessageId() : null,
                    e
            );
        } finally {
            chatProcessingLeaseService.release(dispatchLease);
        }
    }

    @Scheduled(fixedDelayString = "${chat.stream.dispatch-recovery-fixed-delay-ms:1000}")
    public void recoverQueuedMessages() {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            return;
        }

        List<Long> threadIds = chatPersistenceService.findThreadIdsHavingQueuedUserMessages(
                Math.max(1, chatStreamProperties.getDispatchRecoveryBatchSize())
        );

        for (Long threadId : threadIds) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                return;
            }

            dispatchHeadOfLine(threadId);
        }
    }

    private void revertToQueuedQuietly(Long requestMessageId) {
        try {
            chatPersistenceService.revertToQueued(requestMessageId);
        } catch (Exception e) {
            log.error(
                    "Failed to revert chat message back to QUEUED. requestMessageId={}",
                    requestMessageId,
                    e
            );
        }
    }

    private boolean isInterruptLike(Throwable throwable) {
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
