package kr.inventory.domain.reference.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class VendorException extends BusinessException {

    public VendorException(ErrorModel errorModel ) { super(errorModel); }
}
