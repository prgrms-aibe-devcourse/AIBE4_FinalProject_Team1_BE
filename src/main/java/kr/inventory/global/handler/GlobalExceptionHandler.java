package kr.inventory.global.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import kr.inventory.global.common.ApiResponse;
import kr.inventory.global.exception.BusinessException;
import kr.inventory.global.exception.StorageErrorCode;
import kr.inventory.global.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageException(
            StorageException e,
            HttpServletRequest request
    ) {
        StorageErrorCode ec = e.getErrorCode();
        log.warn("StorageException: code={}, message={}", ec.getCode(), ec.getMessage());

        return ResponseEntity
                .status(ec.getStatus())
                .body(ApiResponse.error(
                        ec.getCode(),
                        ec.getMessage(),
                        getRequestPath(request)
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        log.warn("BusinessException: code={}, message={}",
                e.getErrorModel().getCode(),
                e.getErrorModel().getMessage());

        return ResponseEntity
                .status(e.getErrorModel().getStatus())
                .body(ApiResponse.error(
                        e.getErrorModel().getCode(),
                        e.getErrorModel().getMessage(),
                        getRequestPath(request)
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "입력값 검증에 실패했습니다."
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        log.warn("Validation failed: {}", message);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "VALIDATION_ERROR",
                        message != null ? message : "입력값 검증에 실패했습니다.",
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        String message = e.getConstraintViolations().isEmpty()
                ? "요청값 검증에 실패했습니다."
                : e.getConstraintViolations().iterator().next().getMessage();

        log.warn("Constraint violation: {}", message);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "VALIDATION_ERROR",
                        message,
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        log.warn("Request body parse failed", e);

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "INVALID_REQUEST_BODY",
                        "요청 본문 형식이 올바르지 않습니다.",
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        log.warn("Argument type mismatch: {}", e.getName());

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "INVALID_PARAMETER",
                        "요청 파라미터 형식이 올바르지 않습니다: " + e.getName(),
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        log.warn("Missing request parameter: {}", e.getParameterName());

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "MISSING_PARAMETER",
                        "필수 파라미터가 누락되었습니다: " + e.getParameterName(),
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(
            MissingRequestHeaderException e,
            HttpServletRequest request
    ) {
        log.warn("Missing required header: {}", e.getHeaderName());

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "MISSING_HEADER",
                        "필수 헤더가 누락되었습니다: " + e.getHeaderName(),
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestCookieException(
            MissingRequestCookieException e,
            HttpServletRequest request
    ) {
        log.warn("Missing required cookie: {}", e.getCookieName());

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "MISSING_COOKIE",
                        "필수 쿠키가 누락되었습니다: " + e.getCookieName(),
                        getRequestPath(request)
                )
        );
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException e,
            HttpServletRequest request
    ) {
        log.debug("Client disconnected before async response completed. path={}, reason={}",
                getRequestPath(request),
                e.getMessage());

        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        if (isClientAbortException(e)) {
            log.debug("Client connection aborted. path={}, reason={}", getRequestPath(request), e.getMessage());
            return ResponseEntity.noContent().build();
        }

        log.error("Unexpected exception occurred", e);

        return ResponseEntity.internalServerError().body(
                ApiResponse.error(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다.",
                        getRequestPath(request)
                )
        );
    }

    private boolean isClientAbortException(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            String className = cursor.getClass().getName();
            String message = cursor.getMessage();
            if (className.contains("ClientAbortException")
                    || className.contains("AsyncRequestNotUsableException")
                    || (message != null && message.contains("Broken pipe"))) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String getRequestPath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String requestURI = request.getRequestURI();
        return queryString != null ? requestURI + "?" + queryString : requestURI;
    }
}