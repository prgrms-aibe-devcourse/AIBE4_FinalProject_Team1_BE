package kr.inventory.domain.store.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class StoreException extends BusinessException {
    public StoreException(ErrorModel errorModel) {
        super(errorModel);
    }
}
