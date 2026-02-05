package kr.dontworry.domain.ocr.exception;

import kr.dontworry.global.exception.BusinessException;

public class OcrException extends BusinessException {
    public OcrException(OcrError ocrError) {
        super(ocrError);
    }
}
