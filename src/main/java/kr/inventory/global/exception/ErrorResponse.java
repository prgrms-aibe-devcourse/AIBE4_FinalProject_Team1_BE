package kr.inventory.global.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(
    int status,
    String code,
    String message,
    OffsetDateTime timestamp
) {
    public static ErrorResponse of(ErrorModel errorModel) {
        return new ErrorResponse(
                errorModel.getStatus().value(),
                errorModel.getCode(),
                errorModel.getMessage(),
                OffsetDateTime.now()
        );
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, OffsetDateTime.now());
    }
}
