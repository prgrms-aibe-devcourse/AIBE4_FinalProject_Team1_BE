package kr.inventory.domain.notification.controller.dto.response;

public record NotificationActionResponse(
        boolean success,
        String action,
        Long notificationId,
        Integer affectedCount
) {
    public static NotificationActionResponse readOne(Long notificationId) {
        return new NotificationActionResponse(true, "READ_ONE", notificationId, null);
    }

    public static NotificationActionResponse readAll(int affectedCount) {
        return new NotificationActionResponse(true, "READ_ALL", null, affectedCount);
    }

    public static NotificationActionResponse delete(Long notificationId) {
        return new NotificationActionResponse(true, "DELETE", notificationId, null);
    }
}