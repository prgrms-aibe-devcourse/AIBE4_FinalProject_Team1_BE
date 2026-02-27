package kr.inventory.domain.reference.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class MenuException extends BusinessException {
    public MenuException(ErrorModel errorModel) {
        super(errorModel);
    }
}
