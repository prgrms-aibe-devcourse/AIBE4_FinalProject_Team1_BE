package kr.inventory.domain.dining.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TableErrorCode implements ErrorModel {
    TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "TABLE_001", "해당 매장 테이블이 존재하지 않습니다."),
    SOME_TABLES_NOT_FOUND(HttpStatus.NOT_FOUND, "TABLE_002", "테이블들 중 존재하지 않는 테이블이 몇몇 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
