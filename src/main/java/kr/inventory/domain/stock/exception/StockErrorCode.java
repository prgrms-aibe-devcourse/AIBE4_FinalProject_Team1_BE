package kr.inventory.domain.stock.exception;

import kr.inventory.global.exception.ErrorModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements ErrorModel {
    RECIPE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "레시피 정보가 등록되지 않았습니다."),
    RECIPE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "레시피 데이터 파싱 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
