package kr.dontworry.domain.category.exception;

import kr.dontworry.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CategoryErrorCode implements ErrorModel {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "카테고리를 찾을 수 없습니다."),
    CANNOT_EDIT_DEFAULT_CATEGORY_NAME(HttpStatus.FORBIDDEN, "C002", "기본 카테고리는 수정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
