package kr.inventory.domain.sales.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SalesOrderErrorCode implements ErrorModel {
    SALES_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "SO001", "존재하지 않는 주문입니다."),

    DUPLICATE_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "SO002","이미 처리된 주문입니다. (Idempotency Key 중복)"),
    MENU_NOT_FOUND(HttpStatus.BAD_REQUEST, "SO003","존재하지 않는 메뉴가 포함되어 있습니다."),
    MENU_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "SO004","비활성화된 메뉴는 주문할 수 없습니다."),
    INVALID_SESSION(HttpStatus.BAD_REQUEST, "SO005","유효하지 않은 세션입니다."),
    SESSION_EXPIRED(HttpStatus.BAD_REQUEST, "SO006","만료된 세션입니다."),

    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "SO007","재고가 부족합니다."),
    ORDER_NOT_REFUNDABLE(HttpStatus.CONFLICT, "SO008","환불할 수 없는 주문입니다."),
    ORDER_ALREADY_REFUNDED(HttpStatus.CONFLICT, "SO009","이미 환불된 주문입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
