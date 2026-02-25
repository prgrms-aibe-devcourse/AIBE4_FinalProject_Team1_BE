package kr.inventory.domain.dining.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class QrException extends BusinessException {
    public QrException(ErrorModel errorModel) {
        super(errorModel);
    }
}