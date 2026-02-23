package kr.inventory.domain.catalog.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements ErrorModel {
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "M_001", "메뉴를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
