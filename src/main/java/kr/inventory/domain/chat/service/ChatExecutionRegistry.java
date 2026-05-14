package kr.inventory.domain.chat.service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class ChatExecutionRegistry {

    private final ConcurrentMap<Long, ActiveExecution> activeExecutionsByThreadId = new ConcurrentHashMap<>();
    private final Set<Long> interruptedRequestIds = ConcurrentHashMap.newKeySet();

    public boolean register(Long threadId, Long requestMessageId) {
        if (threadId == null || requestMessageId == null) {
            return false;
        }

        ActiveExecution execution = new ActiveExecution(threadId, requestMessageId, Thread.currentThread());
        ActiveExecution existing = activeExecutionsByThreadId.putIfAbsent(threadId, execution);
        return existing == null || existing.requestMessageId().equals(requestMessageId);
    }

    public void unregister(Long threadId, Long requestMessageId) {
        if (threadId == null || requestMessageId == null) {
            return;
        }

        activeExecutionsByThreadId.computeIfPresent(
                threadId,
                (key, current) -> current.requestMessageId().equals(requestMessageId) ? null : current
        );
        interruptedRequestIds.remove(requestMessageId);
    }

    public boolean hasActiveExecution(Long threadId) {
        return threadId != null && activeExecutionsByThreadId.containsKey(threadId);
    }

    public boolean isInterruptRequested(Long requestMessageId) {
        return requestMessageId != null && interruptedRequestIds.contains(requestMessageId);
    }

    public Optional<InterruptTarget> requestInterrupt(Long threadId, String reason) {
        if (threadId == null) {
            return Optional.empty();
        }

        ActiveExecution execution = activeExecutionsByThreadId.get(threadId);
        if (execution == null) {
            return Optional.empty();
        }

        interruptedRequestIds.add(execution.requestMessageId());
        execution.workerThread().interrupt();

        return Optional.of(new InterruptTarget(
                execution.threadId(),
                execution.requestMessageId(),
                reason
        ));
    }

    private record ActiveExecution(
            Long threadId,
            Long requestMessageId,
            Thread workerThread
    ) {
    }

    public record InterruptTarget(
            Long threadId,
            Long requestMessageId,
            String reason
    ) {
    }
}
