package kr.dontworry.domain.ledger.exception;

import kr.dontworry.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LedgerErrorCode implements ErrorModel {
    LEDGER_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "가계부를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "L002", "해당 가계부에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
