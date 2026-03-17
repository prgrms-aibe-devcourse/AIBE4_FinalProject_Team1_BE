package kr.inventory.domain.analytics.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnalyticsErrorCode implements ErrorModel {
	INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "A001", "조회 시작일은 종료일보다 이전이어야 합니다."),
	INVALID_INTERVAL(HttpStatus.BAD_REQUEST, "A002", "interval은 day, week, month 중 하나여야 합니다."),
	DATE_RANGE_TOO_LONG(HttpStatus.BAD_REQUEST, "A003", "조회 기간은 최대 1년까지 가능합니다."),
	FUTURE_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "A004", "미래 날짜는 조회할 수 없습니다."),
	INVALID_TOP_N(HttpStatus.BAD_REQUEST, "A005", "topN은 1 이상 100 이하여야 합니다."),
	STOCK_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A006", "재고 분석 데이터를 가져오는 중 오류가 발생했습니다."),
	STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "A007", "해당 재고 정보를 찾을 수 없습니다."),

	REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A008","리포트 생성에 실패했습니다."),

	INVALID_MENU_NAME(HttpStatus.BAD_REQUEST, "A009", "메뉴명은 필수입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;
}