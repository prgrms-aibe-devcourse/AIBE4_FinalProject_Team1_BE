package kr.inventory.domain.notification.service.publish;

import kr.inventory.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublishedEventListener {

    private final NotificationService notificationService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationPublishedEvent event) {
        try {
            notificationService.sendRealtime(event.userId(), event.notificationId());
        } catch (Exception ex) {
            log.warn(
                    "[NOTIFICATION] realtime dispatch failed. userId={}, notificationId={}, error={}",
                    event.userId(),
                    event.notificationId(),
                    ex.toString()
            );
        }
    }
}