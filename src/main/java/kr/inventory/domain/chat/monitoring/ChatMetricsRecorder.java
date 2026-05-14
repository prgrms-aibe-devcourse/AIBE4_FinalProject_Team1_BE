package kr.inventory.domain.chat.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import kr.inventory.global.llm.exception.LlmQuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ChatMetricsRecorder {

    private static final String OUTCOME = "outcome";
    private static final String PHASE = "phase";
    private static final String REASON = "reason";
    private static final String DIRECTION = "direction";
    private static final String DUPLICATED = "duplicated";
    private static final String MODEL = "model";
    private static final String ERROR_TYPE = "error_type";

    private final MeterRegistry meterRegistry;

    private final AtomicInteger activeWorkers = new AtomicInteger();
    private final AtomicInteger busyWorkers = new AtomicInteger();
    private final AtomicLong streamBacklog = new AtomicLong();

    @jakarta.annotation.PostConstruct
    public void registerGauges() {
        Gauge.builder("chat.worker.active", activeWorkers, AtomicInteger::get)
                .description("Active chat stream worker count")
                .register(meterRegistry);
        Gauge.builder("chat.worker.busy", busyWorkers, AtomicInteger::get)
                .description("Busy chat stream worker count")
                .register(meterRegistry);
        Gauge.builder("chat.stream.backlog", streamBacklog, AtomicLong::get)
                .description("Current Redis Stream backlog size for chat messages")
                .register(meterRegistry);
    }

    public void updateWorkerState(int activeWorkerCount, int busyWorkerCount, long backlogSize) {
        activeWorkers.set(Math.max(0, activeWorkerCount));
        busyWorkers.set(Math.max(0, busyWorkerCount));
        streamBacklog.set(Math.max(0L, backlogSize));
    }

    public void recordAccepted(boolean duplicated) {
        Counter.builder("chat.message.accepted")
                .description("Accepted chat user messages")
                .tag(DUPLICATED, String.valueOf(duplicated))
                .register(meterRegistry)
                .increment();
    }

    public void recordStreamPublished() {
        Counter.builder("chat.stream.published")
                .description("Published chat messages to Redis Stream")
                .register(meterRegistry)
                .increment();
    }

    public void recordScaleEvent(String direction) {
        Counter.builder("chat.worker.scale.events")
                .description("Chat worker scale up/down events")
                .tag(DIRECTION, normalizeTag(direction, "unknown"))
                .register(meterRegistry)
                .increment();
    }

    public void recordQueueWait(Long queuedAtEpochMillis) {
        if (queuedAtEpochMillis == null || queuedAtEpochMillis <= 0L) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - queuedAtEpochMillis;
        if (elapsedMillis < 0L) {
            return;
        }

        Timer.builder("chat.queue.wait")
                .description("Time from Redis Stream enqueue to worker processing start")
                .register(meterRegistry)
                .record(elapsedMillis, TimeUnit.MILLISECONDS);
    }

    public void recordProcessingDuration(long startedNanoTime, String outcome) {
        recordTimer("chat.worker.processing", OUTCOME, normalizeTag(outcome, "unknown"), startedNanoTime);
    }

    public void recordLlmDuration(String phase, long startedNanoTime) {
        recordLlmDuration(phase, "success", startedNanoTime);
    }

    public void recordLlmDuration(String phase, String outcome, long startedNanoTime) {
        long elapsedNanos = System.nanoTime() - startedNanoTime;
        if (elapsedNanos < 0L) {
            return;
        }

        Timer.builder("chat.llm.duration")
                .description("LLM call duration by phase and outcome")
                .tag(PHASE, normalizeTag(phase, "unknown"))
                .tag(OUTCOME, normalizeTag(outcome, "unknown"))
                .register(meterRegistry)
                .record(Duration.ofNanos(elapsedNanos));
    }

    public void recordLlmCall(String phase, String outcome, String model, Throwable throwable) {
        Counter.builder("chat.llm.calls")
                .description("LLM call count by phase, outcome, model, and normalized error type")
                .tag(PHASE, normalizeTag(phase, "unknown"))
                .tag(OUTCOME, normalizeTag(outcome, "unknown"))
                .tag(MODEL, normalizeTag(model, "unknown"))
                .tag(ERROR_TYPE, classifyError(throwable))
                .register(meterRegistry)
                .increment();
    }

    public void recordWorkerSkipped(String reason) {
        Counter.builder("chat.worker.skipped")
                .description("Skipped chat stream records by worker")
                .tag(REASON, normalizeTag(reason, "unknown"))
                .register(meterRegistry)
                .increment();
    }

    public void recordInterruptRequested() {
        Counter.builder("chat.interrupt.requested")
                .description("Requested chat answer interruptions")
                .register(meterRegistry)
                .increment();
    }

    private void recordTimer(String metricName, String tagName, String tagValue, long startedNanoTime) {
        long elapsedNanos = System.nanoTime() - startedNanoTime;
        if (elapsedNanos < 0L) {
            return;
        }

        Timer.builder(metricName)
                .tag(tagName, tagValue)
                .register(meterRegistry)
                .record(Duration.ofNanos(elapsedNanos));
    }

    private String classifyError(Throwable throwable) {
        if (throwable == null) {
            return "none";
        }

        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof LlmQuotaExceededException) {
                return "quota";
            }

            String message = cursor.getMessage();
            if (StringUtils.hasText(message)) {
                String lower = message.toLowerCase();
                if (lower.contains("quota") || lower.contains("429") || lower.contains("resource_exhausted")) {
                    return "quota";
                }
                if (lower.contains("timeout") || lower.contains("timed out")) {
                    return "timeout";
                }
                if (lower.contains("rate limit") || lower.contains("too many requests")) {
                    return "rate_limit";
                }
            }

            if (cursor instanceof InterruptedException) {
                return "interrupted";
            }
            cursor = cursor.getCause();
        }

        return "unknown";
    }

    private String normalizeTag(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_.-]", "_");
    }
}
