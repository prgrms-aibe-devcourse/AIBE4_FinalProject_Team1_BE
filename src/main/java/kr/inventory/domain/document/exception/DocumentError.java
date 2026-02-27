package kr.inventory.domain.document.exception;

import org.springframework.http.HttpStatus;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentError implements ErrorModel {
	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOC01", "해당 문서를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

}
