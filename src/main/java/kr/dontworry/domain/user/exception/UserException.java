package kr.dontworry.domain.user.exception;

import kr.dontworry.global.exception.BusinessException;
import kr.dontworry.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class UserException extends BusinessException {
    public UserException(ErrorModel errorModel) {
        super(errorModel);
    }
}