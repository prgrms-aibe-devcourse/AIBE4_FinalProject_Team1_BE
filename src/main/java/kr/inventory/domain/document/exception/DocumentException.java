package kr.inventory.domain.document.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class DocumentException extends BusinessException {
	public DocumentException(ErrorModel errorModel) {
		super(errorModel);
	}
}
