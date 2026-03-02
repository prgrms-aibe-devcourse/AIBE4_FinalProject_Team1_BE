package kr.inventory.domain.reference.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class IngredientException extends BusinessException {
    public IngredientException(ErrorModel errorModel) {
        super(errorModel);
    }
}