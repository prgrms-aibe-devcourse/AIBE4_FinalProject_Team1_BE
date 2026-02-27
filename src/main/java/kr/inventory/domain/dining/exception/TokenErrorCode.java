package kr.inventory.domain.dining.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TokenErrorCode implements ErrorModel {
    TOKEN_HASHING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T001", "토큰 해싱 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
