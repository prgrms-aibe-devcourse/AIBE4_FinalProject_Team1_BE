package kr.inventory.domain.reference.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements ErrorModel {
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "메뉴를 찾을 수 없습니다."),
    MENU_ACCESS_DENIED(HttpStatus.FORBIDDEN, "M002", "해당 메뉴에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
