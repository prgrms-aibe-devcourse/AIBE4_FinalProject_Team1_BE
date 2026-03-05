package kr.inventory.domain.stock.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements ErrorModel {
	RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "레시피 정보가 등록되지 않았습니다."),
	RECIPE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "레시피 데이터 파싱 중 오류가 발생했습니다."),
	DRAFT_STOCK_TAKE_NOT_FOUND(HttpStatus.NOT_FOUND, "S003", "대기 중인 실사가 없습니다."),
	STOCK_TAKE_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "S004", "이미 확정된 실사입니다."),
	SHEET_NOT_FOUND(HttpStatus.NOT_FOUND, "S005", "시트가 존재하지 않습니다."),
	ALREADY_CONFIRMED(HttpStatus.CONFLICT, "S006", "이미 확정된 시트입니다."),
	STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "S007", "매장을 찾을 수 없습니다."),
	VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "S008", "거래처를 찾을 수 없습니다."),
	INBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "S009", "입고 내역을 찾을 수 없습니다."),
	INBOUND_NOT_DRAFT_STATUS(HttpStatus.CONFLICT, "S010", "입고 내역이 DRAFT 상태가 아닙니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "S011", "사용자를 찾을 수 없습니다."),
	INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "S012", "등록된 재료를 찾을 수 없습니다."),
	INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "S0013", "현재 재고보다 많은 수량을 폐기할 수 없습니다."),
	INVALID_WASTE_QUANTITY(HttpStatus.BAD_REQUEST, "S014", "폐기 수량은 0보다 커야 합니다.");
	private final HttpStatus status;
	private final String code;
	private final String message;
}
