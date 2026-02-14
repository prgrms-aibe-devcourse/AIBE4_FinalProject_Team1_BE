package kr.inventory.domain.sales.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SalesErrorCode implements ErrorModel {
    SALES_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "SO001", "존재하지 않는 주문입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
