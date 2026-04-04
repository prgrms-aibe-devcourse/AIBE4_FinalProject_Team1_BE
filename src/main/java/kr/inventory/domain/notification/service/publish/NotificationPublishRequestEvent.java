package kr.inventory.domain.notification.service.publish;

public record NotificationPublishRequestEvent(
        NotificationPublishCommand command
) {
}
