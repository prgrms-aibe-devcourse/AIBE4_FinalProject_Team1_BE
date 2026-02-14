package kr.inventory.domain.store.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StoreErrorCode implements ErrorModel {
    STORE_NOT_FOUND_OR_ACCESS_DENIED(HttpStatus.FORBIDDEN, "STORE_403", "해당 매장에 대한 접근 권한이 없거나 존재하지 않는 매장입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
