package kr.inventory.domain.chat.service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kr.inventory.domain.chat.service.stream.ChatStreamProperties;
import kr.inventory.global.constant.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatProcessingLeaseService {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = script(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """
    );

    private static final DefaultRedisScript<Long> RENEW_SCRIPT = script(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatStreamProperties chatStreamProperties;

    private final AtomicInteger heartbeatSequence = new AtomicInteger();

    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(
            1,
            runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "chat-processing-heartbeat-" + heartbeatSequence.incrementAndGet()
                );
                thread.setDaemon(true);
                return thread;
            }
    );

    public ProcessingLease tryAcquire(Long requestMessageId) {
        if (requestMessageId == null) {
            return null;
        }

        String lockKey = requestLockKey(requestMessageId);
        String token = tryAcquireLease(lockKey, leaseTtlSeconds());

        if (token == null) {
            return null;
        }

        return new ProcessingLease(requestMessageId, lockKey, token);
    }

    public ThreadDispatchLease tryAcquireThreadDispatchLease(Long threadId) {
        if (threadId == null) {
            return null;
        }

        String lockKey = threadDispatchLockKey(threadId);
        String token = tryAcquireLease(lockKey, threadDispatchLockTtlSeconds());

        if (token == null) {
            return null;
        }

        return new ThreadDispatchLease(threadId, lockKey, token);
    }

    public ScheduledFuture<?> startHeartbeat(ProcessingLease lease) {
        if (lease == null) {
            return null;
        }

        long renewIntervalSeconds = leaseRenewIntervalSeconds();

        return heartbeatExecutor.scheduleAtFixedRate(
                () -> renewQuietly(lease),
                renewIntervalSeconds,
                renewIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void release(ProcessingLease lease) {
        if (lease == null) {
            return;
        }

        releaseLease(
                lease.lockKey(),
                lease.token(),
                "chat processing lease",
                "requestMessageId=" + lease.requestMessageId()
        );
    }

    public void release(ThreadDispatchLease lease) {
        if (lease == null) {
            return;
        }

        releaseLease(
                lease.lockKey(),
                lease.token(),
                "chat thread dispatch lease",
                "threadId=" + lease.threadId()
        );
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            heartbeatExecutor.shutdownNow();
        }
    }

    private String tryAcquireLease(String lockKey, long ttlSeconds) {
        String token = UUID.randomUUID().toString();

        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                token,
                Duration.ofSeconds(ttlSeconds)
        );

        if (Boolean.TRUE.equals(acquired)) {
            return token;
        }

        return null;
    }

    private void releaseLease(
            String lockKey,
            String token,
            String leaseType,
            String target
    ) {
        try {
            stringRedisTemplate.execute(
                    RELEASE_SCRIPT,
                    List.of(lockKey),
                    token
            );
        } catch (Exception e) {
            log.warn(
                    "Failed to release {}. {}, reason={}",
                    leaseType,
                    target,
                    e.getMessage()
            );
        }
    }

    private void renewQuietly(ProcessingLease lease) {
        try {
            Long renewed = stringRedisTemplate.execute(
                    RENEW_SCRIPT,
                    List.of(lease.lockKey()),
                    lease.token(),
                    String.valueOf(Duration.ofSeconds(leaseTtlSeconds()).toMillis())
            );

            if (renewed == null || renewed == 0L) {
                log.warn(
                        "Chat processing lease heartbeat was not renewed. requestMessageId={}",
                        lease.requestMessageId()
                );
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to renew chat processing lease heartbeat. requestMessageId={}, reason={}",
                    lease.requestMessageId(),
                    e.getMessage()
            );
        }
    }

    private long leaseTtlSeconds() {
        return Math.max(2L, chatStreamProperties.getProcessingLockTtlSeconds());
    }

    private long leaseRenewIntervalSeconds() {
        long ttlSeconds = leaseTtlSeconds();
        long configuredSeconds = Math.max(1L, chatStreamProperties.getProcessingLockRenewIntervalSeconds());
        return Math.min(configuredSeconds, Math.max(1L, ttlSeconds / 2L));
    }

    private long threadDispatchLockTtlSeconds() {
        return Math.max(2L, chatStreamProperties.getThreadDispatchLockTtlSeconds());
    }

    private static DefaultRedisScript<Long> script(String scriptText) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(Long.class);
        return script;
    }

    private String requestLockKey(Long requestMessageId) {
        return WebSocketConstants.PROCESSING_LOCK_PREFIX + requestMessageId;
    }

    private String threadDispatchLockKey(Long threadId) {
        return WebSocketConstants.THREAD_DISPATCH_LOCK_PREFIX + threadId;
    }

    public record ProcessingLease(
            Long requestMessageId,
            String lockKey,
            String token
    ) {
    }

    public record ThreadDispatchLease(
            Long threadId,
            String lockKey,
            String token
    ) {
    }
}
