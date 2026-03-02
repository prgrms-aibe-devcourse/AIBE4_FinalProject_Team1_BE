package kr.inventory.domain.document.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OcrError implements ErrorModel {
	FILE_PROCESSING_ERROR(HttpStatus.BAD_REQUEST, "OCR001", "이미지 파일 처리 중 오류가 발생했습니다."),
	GEMINI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OCR002", "Gemini API 호출 중 오류가 발생했습니다."),
	NO_RESPONSE_FROM_GEMINI(HttpStatus.INTERNAL_SERVER_ERROR, "OCR003", "Gemini API로부터 응답을 받지 못했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
