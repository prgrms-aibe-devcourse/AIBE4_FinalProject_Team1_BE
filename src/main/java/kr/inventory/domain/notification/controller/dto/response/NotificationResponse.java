package kr.inventory.domain.notification.controller.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import kr.inventory.domain.notification.entity.Notification;
import kr.inventory.domain.notification.entity.enums.NotificationType;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String message,
        String deepLink,
        boolean isRead,
        OffsetDateTime readAt,
        JsonNode metadata,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getDeepLink(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getMetadata(),
                notification.getCreatedAt()
        );
    }
}