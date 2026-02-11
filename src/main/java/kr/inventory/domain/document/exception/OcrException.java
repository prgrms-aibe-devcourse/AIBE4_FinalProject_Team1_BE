package kr.inventory.domain.document.exception;

import kr.inventory.global.exception.BusinessException;

public class OcrException extends BusinessException {
	public OcrException(OcrError ocrError) {
		super(ocrError);
	}
}
