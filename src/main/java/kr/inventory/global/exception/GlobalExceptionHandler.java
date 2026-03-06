package kr.inventory.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import kr.inventory.global.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<Void>> handleStorageException(StorageException e, HttpServletRequest request) {
        StorageErrorCode ec = e.getErrorCode();
        log.warn("StorageException: code={}, message={}", ec.getCode(), e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                ec.getStatus().value(),
                ec.getCode(),
                ec.getMessage(),
                getRequestPath(request)
        );
        return ResponseEntity.status(ec.getStatus()).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("BusinessException: code={}, message={}", e.getErrorModel().getCode(), e.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                e.getErrorModel().getStatus().value(),
                e.getErrorModel().getCode(),
                e.getErrorModel().getMessage(),
                getRequestPath(request)
        );
        return ResponseEntity
                .status(e.getErrorModel().getStatus())
                .body(response);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestCookieException(
            MissingRequestCookieException e,
            HttpServletRequest request
    ) {
        log.warn("Missing required cookie: {}", e.getCookieName());
        ApiResponse<Void> response = ApiResponse.error(
                400,
                "MISSING_COOKIE",
                "필수 쿠키가 누락되었습니다: " + e.getCookieName(),
                getRequestPath(request)
        );
        return ResponseEntity
                .status(400)
                .body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(
            MissingRequestHeaderException e,
            HttpServletRequest request
    ) {
        log.warn("Missing required header: {}", e.getHeaderName());
        ApiResponse<Void> response = ApiResponse.error(
                400,
                "MISSING_HEADER",
                "필수 헤더가 누락되었습니다: " + e.getHeaderName(),
                getRequestPath(request)
        );
        return ResponseEntity
                .status(400)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("Validation failed: {}", e.getBindingResult().getAllErrors());
        String message = e.getBindingResult().getAllErrors().isEmpty()
                ? "입력값 검증에 실패했습니다."
                : e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ApiResponse<Void> response = ApiResponse.error(
                400,
                "VALIDATION_ERROR",
                message != null ? message : "입력값 검증에 실패했습니다.",
                getRequestPath(request)
        );
        return ResponseEntity
                .status(400)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception occurred", e);
        ApiResponse<Void> response = ApiResponse.error(
                500,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.",
                getRequestPath(request)
        );
        return ResponseEntity
                .status(500)
                .body(response);
    }

    private String getRequestPath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String requestURI = request.getRequestURI();
        return queryString != null ? requestURI + "?" + queryString : requestURI;
    }
}
