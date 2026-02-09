package kr.inventory.domain.auth.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class AuthException extends BusinessException {
    public AuthException(ErrorModel errorModel) {
        super(errorModel);
    }
}