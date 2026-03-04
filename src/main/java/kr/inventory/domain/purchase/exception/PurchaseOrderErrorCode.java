package kr.inventory.domain.purchase.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PurchaseOrderErrorCode implements ErrorModel {
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "매장을 찾을 수 없습니다."),
    PURCHASE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "발주서를 찾을 수 없습니다."),
    PURCHASE_ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P003", "발주서에 접근할 권한이 없습니다."),
    EMPTY_ITEMS(HttpStatus.BAD_REQUEST, "P004", "발주 품목은 최소 1개 이상이어야 합니다."),
    ALREADY_CANCELED(HttpStatus.CONFLICT, "P005", "이미 취소된 발주서입니다."),
    PDF_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P006", "PDF 생성 중 오류가 발생했습니다."),
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "P007", "거래처를 찾을 수 없습니다."),
    VENDOR_NOT_ACTIVE(HttpStatus.CONFLICT, "P008", "비활성화된 거래처는 발주에 사용할 수 없습니다."),
    VENDOR_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "P009", "해당 거래처는 발주 매장에 속하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
