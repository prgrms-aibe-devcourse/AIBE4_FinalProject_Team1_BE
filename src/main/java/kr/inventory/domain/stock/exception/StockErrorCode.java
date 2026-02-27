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
    VENDOR_NOT_ACTIVE(HttpStatus.CONFLICT, "S012", "비활성화된 거래처는 사용할 수 없습니다."),
    PURCHASE_ORDER_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "S013", "해당 발주서는 현재 매장에 속하지 않습니다."),
    PURCHASE_ORDER_VENDOR_MISMATCH(HttpStatus.BAD_REQUEST, "S014", "입고 거래처와 발주 거래처가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
