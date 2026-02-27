package kr.inventory.domain.dining.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class TokenException extends BusinessException {
    public TokenException(ErrorModel errorModel) {
        super(errorModel);
    }
}