package kr.inventory.domain.chat.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements ErrorModel {

    // 4xx Client Errors
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C002", "접근 권한이 없습니다."),

    // Validation Errors
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "C004", "title은 100자를 초과할 수 없습니다."),
    CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "C005", "content는 비어 있을 수 없습니다."),
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "C006", "content는 4000자를 초과할 수 없습니다."),
    CLIENT_MESSAGE_ID_EMPTY(HttpStatus.BAD_REQUEST, "C007", "clientMessageId는 비어 있을 수 없습니다."),
    CLIENT_MESSAGE_ID_TOO_LONG(HttpStatus.BAD_REQUEST, "C008", "clientMessageId는 100자를 초과할 수 없습니다."),
    CURSOR_INVALID(HttpStatus.BAD_REQUEST, "C009", "cursor는 1 이상이어야 합니다."),

    // Not Found Errors
    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "C010", "존재하지 않거나 접근할 수 없는 채팅 스레드입니다."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "C011", "존재하지 않는 채팅 메시지입니다."),

    // Business Logic Errors
    ALREADY_COMPLETED_WITHOUT_RESPONSE(HttpStatus.BAD_REQUEST, "C012", "이미 완료된 요청인데 응답 메시지가 존재하지 않습니다."),
    ALREADY_FAILED(HttpStatus.BAD_REQUEST, "C013", "이미 실패 처리된 요청입니다."),
    NOT_USER_MESSAGE(HttpStatus.BAD_REQUEST, "C014", "USER 메시지만 요청 메시지로 처리할 수 있습니다."),

    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C015", "알 수 없는 오류가 발생했습니다."),
    ASSISTANT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "C016", "답변을 생성하지 못했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
