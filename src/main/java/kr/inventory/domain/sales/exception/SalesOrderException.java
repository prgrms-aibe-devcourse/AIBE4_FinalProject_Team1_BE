package kr.inventory.domain.sales.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class SalesOrderException extends BusinessException {
    public SalesOrderException(ErrorModel errorModel) {
        super(errorModel);
    }
}