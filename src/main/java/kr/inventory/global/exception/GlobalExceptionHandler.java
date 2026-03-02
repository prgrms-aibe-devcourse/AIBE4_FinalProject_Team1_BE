package kr.inventory.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException e) {
        StorageErrorCode ec = e.getErrorCode();
        log.warn("StorageException: code={}, message={}", ec.getCode(), e.getMessage());
        ErrorResponse body = ErrorResponse.of(
                ec.getStatus().value(),
                ec.getCode(),
                ec.getMessage()
        );
        return ResponseEntity.status(ec.getStatus()).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getErrorModel().getCode(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorModel());
        return ResponseEntity
                .status(e.getErrorModel().getStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(
            MissingRequestCookieException e) {
        log.warn("Missing required cookie: {}", e.getCookieName());
        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                "MISSING_COOKIE",
                "필수 쿠키가 누락되었습니다: " + e.getCookieName()
        );
        return ResponseEntity
                .status(400)
                .body(errorResponse);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
            MissingRequestHeaderException e) {
        log.warn("Missing required header: {}", e.getHeaderName());
        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                "MISSING_HEADER",
                "필수 헤더가 누락되었습니다: " + e.getHeaderName()
        );
        return ResponseEntity
                .status(400)
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getBindingResult().getAllErrors());
        String message = e.getBindingResult().getAllErrors().isEmpty()
                ? "입력값 검증에 실패했습니다."
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                "VALIDATION_ERROR",
                message != null ? message : "입력값 검증에 실패했습니다."
        );
        return ResponseEntity
                .status(400)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        ErrorResponse errorResponse = ErrorResponse.of(
                500,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        );
        return ResponseEntity
                .status(500)
                .body(errorResponse);
    }
}
