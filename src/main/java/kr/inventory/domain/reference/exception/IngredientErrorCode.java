package kr.inventory.domain.reference.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IngredientErrorCode implements ErrorModel {
    INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "재료 정보가 등록되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
