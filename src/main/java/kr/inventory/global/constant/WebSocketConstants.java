package kr.inventory.global.constant;

public final class WebSocketConstants {

    // Authorization 헤더
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_HEADER_LOWER_CASE = "authorization";
    public static final String TOKEN_HEADER = "token";
    public static final String ACCESS_TOKEN_HEADER = "accessToken";
    public static final String BEARER_PREFIX = "Bearer ";

    // WebSocket 세션 속성
    public static final String SESSION_AUTHORIZATION = "WS_AUTHORIZATION";
    public static final String SESSION_ACCESS_TOKEN = "WS_ACCESS_TOKEN";

    // Query 파라미터
    public static final String TOKEN_PARAM = "token";
    public static final String ACCESS_TOKEN_PARAM = "accessToken";

    // Redis Stream 명령어
    public static final String CMD_XGROUP = "XGROUP";
    public static final String CMD_XAUTOCLAIM = "XAUTOCLAIM";
    public static final String CMD_XACK = "XACK";
    public static final String CMD_XDEL = "XDEL";
    public static final String XGROUP_CREATE = "CREATE";
    public static final String XGROUP_START_ID = "0-0";
    public static final String XGROUP_MKSTREAM = "MKSTREAM";
    public static final String CLAIM_START_ID = "0-0";
    public static final String CLAIM_COUNT = "COUNT";

    // 리소스 키 prefix
    public static final String PROCESSING_LOCK_PREFIX = "chat:processing:lock:";
    public static final String THREAD_DISPATCH_LOCK_PREFIX = "chat:thread:dispatch:lock:";

    // 에러 메시지
    public static final String ERROR_NO_USER_INFO = "인증된 사용자 정보를 찾을 수 없습니다.";
    public static final String ERROR_INVALID_USER_ID = "사용자 식별자를 해석할 수 없습니다.";
}
