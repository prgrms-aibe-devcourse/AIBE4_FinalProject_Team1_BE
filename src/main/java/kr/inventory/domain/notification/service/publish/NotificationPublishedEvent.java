package kr.inventory.domain.notification.service.publish;

public record NotificationPublishedEvent(
        Long userId,
        Long notificationId
) {
    public static NotificationPublishedEvent from(Long userId, Long notificationId) {
        return new NotificationPublishedEvent(userId, notificationId);
    }
}