package kr.inventory.domain.notification.service.publish;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationAsyncHandler {
    private final NotificationPublishService notificationPublishService;

    @Async("notificationTaskExecutor")
    @EventListener
    public void handleNotificationEvent(NotificationPublishRequestEvent event){
        notificationPublishService.publish(event.command());
    }
}
