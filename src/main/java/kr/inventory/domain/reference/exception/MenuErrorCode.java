package kr.inventory.domain.reference.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements ErrorModel {
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "메뉴를 찾을 수 없습니다."),
    MENU_ACCESS_DENIED(HttpStatus.FORBIDDEN, "M002", "해당 메뉴에 대한 접근 권한이 없습니다."),
    DUPLICATE_MENU_NAME(HttpStatus.CONFLICT, "M003", "이미 존재하는 메뉴 이름입니다."),
    INGREDIENT_IN_USE_BY_ACTIVE_MENU(HttpStatus.CONFLICT, "M004", "활성화 되어있는 메뉴 중 해당 재료를 사용하는 메뉴가 존재합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
