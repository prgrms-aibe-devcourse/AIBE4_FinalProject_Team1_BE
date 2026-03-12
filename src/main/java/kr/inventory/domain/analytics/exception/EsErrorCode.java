package kr.inventory.domain.analytics.exception;

import org.springframework.http.HttpStatus;

import kr.inventory.global.exception.ErrorModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EsErrorCode implements ErrorModel {
	STOCK_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "재고 분석 데이터를 가져오는 중 오류가 발생했습니다."),
	STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "해당 재고 정보를 찾을 수 없습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}
