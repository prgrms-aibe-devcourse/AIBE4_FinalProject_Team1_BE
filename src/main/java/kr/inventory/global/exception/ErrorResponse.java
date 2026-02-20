package kr.inventory.global.exception;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
}
