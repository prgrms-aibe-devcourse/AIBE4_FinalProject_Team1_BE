package kr.inventory.domain.stock.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class StockException extends BusinessException {
    public StockException(ErrorModel errorModel) {
        super(errorModel);
    }
}