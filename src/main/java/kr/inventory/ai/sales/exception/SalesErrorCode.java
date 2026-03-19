package kr.inventory.ai.sales.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SalesErrorCode implements ErrorModel {
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "SAI001", "시작 날짜가 종료 날짜보다 늦을 수 없습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "SAI002", "날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식이어야 합니다."),
    BOTH_DATES_REQUIRED(HttpStatus.BAD_REQUEST, "SAI003", "fromDate와 toDate는 함께 제공해야 합니다."),
    UNSUPPORTED_PRESET(HttpStatus.BAD_REQUEST, "SAI004", "지원하지 않는 기간 프리셋입니다."),
    INVALID_LEDGER_STATUS(HttpStatus.BAD_REQUEST, "SAI005", "status는 COMPLETED 또는 REFUNDED만 지원합니다."),
    INVALID_LEDGER_TYPE(HttpStatus.BAD_REQUEST, "SAI006", "type은 DINE_IN 또는 TAKEOUT만 지원합니다."),
    INVALID_AMOUNT_RANGE(HttpStatus.BAD_REQUEST, "SAI007", "amountMin은 amountMax보다 클 수 없습니다."),
    INVALID_SORT_BY(HttpStatus.BAD_REQUEST, "SAI008", "sortBy는 ordered_at_desc, ordered_at_asc, total_amount_desc, total_amount_asc 중 하나여야 합니다."),
    INVALID_SELECTION_INDEX(HttpStatus.BAD_REQUEST, "SAI009", "selectionIndex는 1 이상이어야 합니다."),
    ORDER_DETAIL_LOOKUP_REQUIRES_CONDITION(HttpStatus.BAD_REQUEST, "SAI010", "orderPublicId 없이 상세 조회하려면 조회 조건이나 기간을 함께 제공해야 합니다."),
    SALES_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "SAI011", "조건에 맞는 주문 기록을 찾을 수 없습니다."),
    INVALID_MENU_NAME(HttpStatus.BAD_REQUEST, "SAI012", "menuName은 비어 있을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
