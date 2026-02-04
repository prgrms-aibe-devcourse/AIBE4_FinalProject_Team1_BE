package kr.dontworry.domain.category.exception;

import kr.dontworry.global.exception.BusinessException;
import kr.dontworry.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class CategoryException extends BusinessException {
    public CategoryException(ErrorModel errorModel) {
        super(errorModel);
    }
}