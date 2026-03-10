package kr.inventory.domain.notification.service.publish;

public enum NotificationDisplayPolicy {
    TOAST_AND_INBOX, // 알림함O, 토스트O

    INBOX_ONLY // 알림함O, 토스트X
}