package kr.inventory.domain.notification.constant;

public final class NotificationConstants {

    // 페이지네이션 제약조건
    public static final int MIN_PAGE = 0;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;

    // SSE 설정
    public static final long DEFAULT_TIMEOUT = 60L * 60L * 1000L;
    public static final String CONNECT_EVENT_NAME = "connect";
    public static final String NOTIFICATION_EVENT_NAME = "notification";
}
