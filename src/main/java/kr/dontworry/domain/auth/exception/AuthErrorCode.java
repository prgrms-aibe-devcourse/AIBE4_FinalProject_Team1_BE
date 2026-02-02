package kr.dontworry.domain.auth.exception;

import kr.dontworry.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorModel {
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A002", "저장된 리프레시 토큰을 찾을 수 없습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "A003", "리프레시 토큰이 일치하지 않습니다. 보안 위험으로 기존 토큰을 삭제합니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "A004", "지원하지 않는 소셜 로그인 제공자입니다."),
    LOGOUT_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "이미 로그아웃 처리된 토큰입니다. 다시 로그인해주세요."),
    INVALID_AUTH_CODE(HttpStatus.UNAUTHORIZED, "A006", "유효하지 않은 코드입니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "A007", "유효하지 않은 토큰입니다."),
    INVALID_TOKEN_FORMAT(HttpStatus.BAD_REQUEST, "A008", "유효하지 않은 토큰 형식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
