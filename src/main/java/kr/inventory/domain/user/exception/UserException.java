package kr.inventory.domain.user.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;

@Getter
public class UserException extends BusinessException {
    public UserException(ErrorModel errorModel) {
        super(errorModel);
    }
}