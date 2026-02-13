package kr.inventory.domain.sales.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class SalesException extends BusinessException {
    public SalesException(ErrorModel errorModel) {
        super(errorModel);
    }
}