package kr.inventory.ai.sales.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SalesErrorCode implements ErrorModel {

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "S001", "시작 날짜가 종료 날짜보다 늦을 수 없습니다."),
    INVALID_BASE_DATE_RANGE(HttpStatus.BAD_REQUEST, "S002", "비교 기준 시작 날짜가 종료 날짜보다 늦을 수 없습니다."),
    UNSUPPORTED_COMPARE_MODE(HttpStatus.BAD_REQUEST, "S003", "지원하지 않는 비교 모드입니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "S004", "날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식이어야 합니다."),
    BOTH_DATES_REQUIRED(HttpStatus.BAD_REQUEST, "S005", "시작 날짜와 종료 날짜를 모두 제공해야 합니다."),
    UNSUPPORTED_PRESET(HttpStatus.BAD_REQUEST, "S006", "지원하지 않는 기간 설정입니다."),
    PERIOD_AND_DATES_EXCLUSIVE(HttpStatus.BAD_REQUEST, "S007", "기간 프리셋과 날짜 범위를 동시에 사용할 수 없습니다."),
    INVALID_VIEW_TYPE(HttpStatus.BAD_REQUEST, "S008", "viewType은 combined, day_only, hour_only 중 하나여야 합니다."),
    INVALID_INTERVAL(HttpStatus.BAD_REQUEST, "S009", "interval은 day, week, month 중 하나여야 합니다."),
    INVALID_METRIC(HttpStatus.BAD_REQUEST, "S010", "metric은 amount, order_count, both 중 하나여야 합니다."),
    INVALID_RANK_BY(HttpStatus.BAD_REQUEST, "S011", "rankBy는 quantity 또는 amount여야 합니다."),
    BASE_DATES_REQUIRED_FOR_CUSTOM_COMPARE(HttpStatus.BAD_REQUEST, "S012", "custom 비교 모드에는 baseFromDate와 baseToDate가 필요합니다."),
    BASE_DATES_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "S013", "custom이 아닌 비교 모드에는 baseFromDate와 baseToDate를 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
