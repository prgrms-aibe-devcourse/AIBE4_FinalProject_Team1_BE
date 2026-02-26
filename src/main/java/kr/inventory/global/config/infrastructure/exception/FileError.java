package kr.inventory.global.config.infrastructure.exception;

import org.springframework.http.HttpStatus;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileError implements ErrorModel {
	INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "STORAGE-01", "지원하지 않는 파일 확장자입니다. (JPG, PNG, PDF, EXCEL만 가능)"),
	FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "STORAGE-02", "업로드 가능한 최대 파일 용량을 초과했습니다."),
	STORAGE_UPLOAD_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE-03", "스토리지 서버 업로드 중 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
