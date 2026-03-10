package kr.inventory.domain.notification.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorModel {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "N001", "인증 정보가 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "N002", "잘못된 요청입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N003", "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
