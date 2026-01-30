package kr.dontworry.domain.user.exception;

import kr.dontworry.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorModel {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 유저입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
