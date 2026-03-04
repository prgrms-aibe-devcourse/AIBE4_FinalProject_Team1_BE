package kr.inventory.domain.purchase.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class PurchaseOrderException extends BusinessException {
    public PurchaseOrderException(ErrorModel errorModel) {
        super(errorModel);
    }
}
