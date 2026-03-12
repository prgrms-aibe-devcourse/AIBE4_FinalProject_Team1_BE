package kr.inventory.domain.chat.constant;

public final class ChatConstants {

    // 제약조건
    public static final int MAX_TITLE_LENGTH = 100;
    public static final String DEFAULT_THREAD_TITLE = "새 대화";
    public static final int MAX_CONTENT_LENGTH = 4000;
    public static final int MAX_CLIENT_MESSAGE_ID_LENGTH = 100;
    public static final int MAX_ERROR_LENGTH = 500;

    // 페이지네이션
    public static final int MESSAGE_PAGE_SIZE = 30;

    // 프롬프트
    public static final int DEFAULT_CONTEXT_SIZE = 20;

    // WebSocket
    public static final String USER_QUEUE_DESTINATION = "/queue/chat";

    // Redis Stream
    public static final String TYPE = "type";
    public static final String USER_ID = "userId";
    public static final String THREAD_ID = "threadId";
    public static final String REQUEST_MESSAGE_ID = "requestMessageId";
    public static final String CLIENT_MESSAGE_ID = "clientMessageId";
    public static final String CONTENT = "content";
    public static final String CMD_XGROUP = "XGROUP";
    public static final String XGROUP_CREATE = "CREATE";
    public static final String XGROUP_START_ID = "$";
    public static final String XGROUP_MKSTREAM = "MKSTREAM";
}
