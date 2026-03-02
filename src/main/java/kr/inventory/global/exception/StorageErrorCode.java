package kr.inventory.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode {
	INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "S001", "지원하지 않는 파일 확장자입니다. (JPG, PNG, PDF, EXCEL만 가능)"),
	STORAGE_UPLOAD_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "스토리지 서버 업로드 중 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
