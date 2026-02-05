package kr.dontworry.domain.ledger.exception;

import kr.dontworry.global.exception.BusinessException;
import kr.dontworry.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class LedgerException extends BusinessException {
    public LedgerException(ErrorModel errorModel) {
        super(errorModel);
    }
}