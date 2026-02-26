package kr.inventory.global.config.infrastructure.exception;

import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.ErrorModel;

public class FileException extends BusinessException {
	public FileException(ErrorModel errorModel) {
		super(errorModel);
	}
}
