package kr.inventory.domain.chat.service.stream;

import jakarta.annotation.PreDestroy;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import kr.inventory.domain.chat.service.ChatWorkerService;
import kr.inventory.global.constant.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatStreamConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ChatStreamProperties chatStreamProperties;
    private final ChatWorkerService chatWorkerService;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final AtomicInteger workerSequence = new AtomicInteger();
    private final AtomicInteger workerThreadSequence = new AtomicInteger();

    private final Map<Integer, WorkerSlot> workers = new ConcurrentHashMap<>();

    private final ExecutorService workerExecutor = Executors.newCachedThreadPool(
            runnable -> new Thread(
                    runnable,
                    "chat-stream-worker-thread-" + workerThreadSequence.incrementAndGet()
            )
    );

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (started.compareAndSet(false, true)) {
            scaleUpTo(normalizeWorkerCount(chatStreamProperties.getMinWorkers()));
        }
    }

    @Scheduled(fixedDelayString = "${chat.stream.scale-check-fixed-delay-ms:1000}")
    public void reconcileWorkers() {
        if (!started.get() || stopping.get()) {
            return;
        }

        synchronized (this) {
            int desiredWorkers = calculateDesiredWorkerCount();
            scaleUpTo(desiredWorkers);
            scaleDownTo(desiredWorkers);
        }
    }

    @PreDestroy
    public void shutdown() {
        stopping.set(true);
        started.set(false);

        List<WorkerSlot> snapshot = new ArrayList<>(workers.values());
        for (WorkerSlot worker : snapshot) {
            worker.requestStop();
            Future<?> future = worker.future();
            if (future != null) {
                future.cancel(true);
            }
        }

        workerExecutor.shutdown();
        try {
            if (!workerExecutor.awaitTermination(
                    Math.max(1L, chatStreamProperties.getGracefulShutdownWaitSeconds()),
                    TimeUnit.SECONDS
            )) {
                workerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            workerExecutor.shutdownNow();
        }
    }

    private int calculateDesiredWorkerCount() {
        long backlog = outstandingStreamSize();
        if (backlog <= 0L) {
            return normalizeWorkerCount(chatStreamProperties.getMinWorkers());
        }

        long bounded = Math.min(backlog, chatStreamProperties.getMaxWorkers());
        return normalizeWorkerCount((int) bounded);
    }

    private long outstandingStreamSize() {
        try {
            Long size = redisTemplate.opsForStream().size(chatStreamProperties.getKey());
            return size == null ? 0L : size;
        } catch (Exception e) {
            log.debug("Failed to read chat stream size. reason={}", e.getMessage());
            return 0L;
        }
    }

    private void scaleUpTo(int desiredWorkers) {
        while (workers.size() < desiredWorkers && workers.size() < chatStreamProperties.getMaxWorkers()) {
            startWorker();
        }
    }

    private void scaleDownTo(int desiredWorkers) {
        if (workers.size() <= desiredWorkers) {
            return;
        }

        long idleThresholdMillis = TimeUnit.SECONDS.toMillis(
                Math.max(1L, chatStreamProperties.getScaleDownIdleSeconds())
        );

        List<WorkerSlot> candidates = workers.values().stream()
                .filter(worker -> !worker.isBusy())
                .filter(worker -> worker.isIdleFor(idleThresholdMillis))
                .sorted((left, right) -> Long.compare(left.idleSince(), right.idleSince()))
                .toList();

        int remaining = workers.size();
        for (WorkerSlot worker : candidates) {
            if (remaining <= desiredWorkers) {
                break;
            }

            worker.requestStop();
            Future<?> future = worker.future();
            if (future != null) {
                future.cancel(true);
            }
            remaining--;
        }
    }

    private void startWorker() {
        int workerId = workerSequence.incrementAndGet();
        String consumerName = chatStreamProperties.getConsumerName() + "-" + workerId;

        WorkerSlot worker = new WorkerSlot(workerId, consumerName);
        workers.put(workerId, worker);

        Future<?> future = workerExecutor.submit(() -> runWorker(worker));
        worker.future(future);

        log.info(
                "Started chat stream worker. consumerName={}, activeWorkers={}",
                consumerName,
                workers.size()
        );
    }

    private void runWorker(WorkerSlot worker) {
        try {
            while (worker.isRunning() && !stopping.get()) {
                if (Thread.currentThread().isInterrupted() && !worker.isRunning()) {
                    break;
                }

                ChatStreamRecord record = pollNextRecord(worker.consumerName());
                if (record == null) {
                    worker.markIdle();
                    continue;
                }

                worker.markBusy();
                try {
                    boolean shouldAcknowledge = handleOne(record);
                    if (shouldAcknowledge) {
                        acknowledgeAndDelete(record.recordId());
                    }
                } catch (Exception e) {
                    if (isInterruptLike(e)) {
                        Thread.currentThread().interrupt();
                        if (!worker.isRunning() || stopping.get()) {
                            break;
                        }
                        continue;
                    }

                    log.error("Failed to finish chat stream record. recordId={}", record.recordId(), e);
                } finally {
                    worker.markIdle();
                }
            }
        } catch (Exception e) {
            if (isInterruptLike(e)) {
                Thread.currentThread().interrupt();
            } else {
                log.error(
                        "Chat stream worker crashed. consumerName={}",
                        worker.consumerName(),
                        e
                );
            }
        } finally {
            workers.remove(worker.workerId(), worker);
            log.info(
                    "Stopped chat stream worker. consumerName={}, activeWorkers={}",
                    worker.consumerName(),
                    workers.size()
            );
        }
    }

    private ChatStreamRecord pollNextRecord(String consumerName) {
        ChatStreamRecord claimedRecord = claimStaleRecord(consumerName);
        if (claimedRecord != null) {
            return claimedRecord;
        }

        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                Consumer.from(
                        chatStreamProperties.getConsumerGroup(),
                        consumerName
                ),
                StreamReadOptions.empty()
                        .count(1)
                        .block(Duration.ofSeconds(chatStreamProperties.getPollBlockSeconds())),
                StreamOffset.create(chatStreamProperties.getKey(), ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return null;
        }

        return ChatStreamRecord.from(records.get(0));
    }

    private ChatStreamRecord claimStaleRecord(String consumerName) {
        if (chatStreamProperties.getClaimMinIdleSeconds() < 1L) {
            return null;
        }

        try {
            Object rawResponse = redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.execute(
                            WebSocketConstants.CMD_XAUTOCLAIM,
                            b(chatStreamProperties.getKey()),
                            b(chatStreamProperties.getConsumerGroup()),
                            b(consumerName),
                            b(String.valueOf(TimeUnit.SECONDS.toMillis(chatStreamProperties.getClaimMinIdleSeconds()))),
                            b(WebSocketConstants.CLAIM_START_ID),
                            b(WebSocketConstants.CLAIM_COUNT),
                            b(String.valueOf(Math.max(1L, chatStreamProperties.getClaimCount())))
                    )
            );

            return parseFirstClaimedRecord(rawResponse);
        } catch (Exception e) {
            if (!isInterruptLike(e)) {
                log.debug("Failed to auto-claim stale chat record. reason={}", e.getMessage());
            }
            return null;
        }
    }

    private ChatStreamRecord parseFirstClaimedRecord(Object rawResponse) {
        if (!(rawResponse instanceof List<?> response) || response.size() < 2) {
            return null;
        }

        Object claimedEntries = response.get(1);
        if (!(claimedEntries instanceof List<?> entries) || entries.isEmpty()) {
            return null;
        }

        for (Object entry : entries) {
            ChatStreamRecord record = toClaimedRecord(entry);
            if (record != null) {
                return record;
            }
        }

        return null;
    }

    private ChatStreamRecord toClaimedRecord(Object rawEntry) {
        if (!(rawEntry instanceof List<?> entry) || entry.size() < 2) {
            return null;
        }

        String recordId = asString(entry.get(0));
        Map<String, String> value = asStringMap(entry.get(1));
        if (!StringUtils.hasText(recordId) || value.isEmpty()) {
            return null;
        }

        return new ChatStreamRecord(recordId, value);
    }

    private boolean handleOne(ChatStreamRecord record) {
        try {
            ChatStreamUserMessagePayload payload = ChatStreamUserMessagePayload.fromMap(record.value());

            if (payload.type() == ChatStreamMessageType.INIT) {
                return true;
            }

            if (payload.requestMessageId() == null || payload.threadId() == null || payload.userId() == null) {
                log.error(
                        "Malformed chat stream payload. recordId={}, payload={}",
                        record.recordId(),
                        record.value()
                );
                return true;
            }

            return chatWorkerService.process(payload);
        } catch (Exception e) {
            if (isInterruptLike(e)) {
                Thread.currentThread().interrupt();
                return false;
            }

            log.error("Failed to handle chat stream record. recordId={}", record.recordId(), e);
            return true;
        }
    }

    private void acknowledgeAndDelete(String recordId) {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.execute(
                    WebSocketConstants.CMD_XACK,
                    b(chatStreamProperties.getKey()),
                    b(chatStreamProperties.getConsumerGroup()),
                    b(recordId)
            );
            connection.execute(
                    WebSocketConstants.CMD_XDEL,
                    b(chatStreamProperties.getKey()),
                    b(recordId)
            );
            return null;
        });
    }

    private Map<String, String> asStringMap(Object rawValue) {
        Map<String, String> value = new LinkedHashMap<>();

        if (rawValue instanceof Map<?, ?> sourceMap) {
            for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                String key = asString(entry.getKey());
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                value.put(key, asString(entry.getValue()));
            }
            return value;
        }

        if (rawValue instanceof List<?> flatList) {
            for (int index = 0; index + 1 < flatList.size(); index += 2) {
                String key = asString(flatList.get(index));
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                value.put(key, asString(flatList.get(index + 1)));
            }
        }

        return value;
    }

    private String asString(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(rawValue);
    }

    private int normalizeWorkerCount(int requestedWorkers) {
        int minWorkers = Math.max(1, chatStreamProperties.getMinWorkers());
        int maxWorkers = Math.max(minWorkers, chatStreamProperties.getMaxWorkers());

        if (requestedWorkers < minWorkers) {
            return minWorkers;
        }
        if (requestedWorkers > maxWorkers) {
            return maxWorkers;
        }
        return requestedWorkers;
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

    private byte[] b(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record ChatStreamRecord(
            String recordId,
            Map<String, String> value
    ) {
        private static ChatStreamRecord from(MapRecord<String, Object, Object> record) {
            Map<String, String> value = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> entry : record.getValue().entrySet()) {
                String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                String content = entry.getValue() == null ? null : String.valueOf(entry.getValue());
                value.put(key, content);
            }
            return new ChatStreamRecord(record.getId().getValue(), value);
        }
    }

    private static final class WorkerSlot {

        private final int workerId;
        private final String consumerName;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicBoolean busy = new AtomicBoolean(false);
        private final AtomicLong idleSince = new AtomicLong(System.currentTimeMillis());

        private volatile Future<?> future;

        private WorkerSlot(int workerId, String consumerName) {
            this.workerId = workerId;
            this.consumerName = consumerName;
        }

        private int workerId() {
            return workerId;
        }

        private String consumerName() {
            return consumerName;
        }

        private boolean isRunning() {
            return running.get();
        }

        private void requestStop() {
            running.set(false);
        }

        private void markBusy() {
            busy.set(true);
        }

        private void markIdle() {
            if (busy.getAndSet(false)) {
                idleSince.set(System.currentTimeMillis());
            }
        }

        private boolean isBusy() {
            return busy.get();
        }

        private boolean isIdleFor(long idleThresholdMillis) {
            return !busy.get() && System.currentTimeMillis() - idleSince.get() >= idleThresholdMillis;
        }

        private long idleSince() {
            return idleSince.get();
        }

        private Future<?> future() {
            return future;
        }

        private void future(Future<?> future) {
            this.future = future;
        }
    }
}
