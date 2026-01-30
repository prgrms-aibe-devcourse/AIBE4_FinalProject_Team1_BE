package kr.dontworry.auth.exception;

import kr.dontworry.global.exception.BusinessException;
import kr.dontworry.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class AuthException extends BusinessException {
    public AuthException(ErrorModel errorModel) {
        super(errorModel);
    }
}