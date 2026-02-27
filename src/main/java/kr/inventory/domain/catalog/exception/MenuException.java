package kr.inventory.domain.catalog.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class MenuException extends BusinessException {
    public MenuException(ErrorModel errorModel) {
        super(errorModel);
    }
}
