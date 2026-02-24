package kr.inventory.global.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(
    int status,
    String code,
    String message,
    OffsetDateTime timestamp
) {
}
