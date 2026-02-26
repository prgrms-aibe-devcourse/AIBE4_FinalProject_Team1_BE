package kr.inventory.domain.document.exception;

import org.springframework.http.HttpStatus;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentError implements ErrorModel {
	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOC-01", "해당 문서를 찾을 수 없습니다."),
	DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOC-02", "해당 문서에 대한 접근 권한이 없습니다."),

	INVALID_DOCUMENT_STATUS(HttpStatus.BAD_REQUEST, "DOC-11", "변경할 수 없는 문서 상태입니다."),
	ALREADY_PROCESSED_DOCUMENT(HttpStatus.BAD_REQUEST, "DOC-12", "이미 처리가 완료된 문서입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}
