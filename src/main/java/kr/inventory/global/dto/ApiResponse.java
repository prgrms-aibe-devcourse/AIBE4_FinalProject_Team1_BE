package kr.inventory.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

/**
 * 모든 API 응답의 표준 형식.
 * - 성공/실패 모두 동일한 형식으로 응답
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,       // "success" | "error"
        String code,         // HTTP 상태 코드 또는 에러 코드
        String message,      // 에러 메시지 (성공 시 null)
        String path,         // 요청 경로
        OffsetDateTime timestamp,
        T data               // 실제 응답 데이터 (실패 시 null)
) {

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data, String path) {
        return new ApiResponse<>(
                "success",
                "OK",
                null,
                path,
                OffsetDateTime.now(),
                data
        );
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> success(String path) {
        return new ApiResponse<>(
                "success",
                "OK",
                null,
                path,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 에러 응답 생성
     */
    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return new ApiResponse<>(
                "error",
                code,
                message,
                path,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 에러 응답 생성 (HTTP 상태 코드 포함)
     */
    public static <T> ApiResponse<T> error(int status, String code, String message, String path) {
        return new ApiResponse<>(
                "error",
                code,
                message,
                path,
                OffsetDateTime.now(),
                null
        );
    }
}
