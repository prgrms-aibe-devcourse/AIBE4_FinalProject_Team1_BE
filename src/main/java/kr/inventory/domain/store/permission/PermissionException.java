package kr.inventory.domain.store.permission;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class PermissionException extends BusinessException {

    public PermissionException(ErrorModel errorModel) {
        super(errorModel);
    }
}
