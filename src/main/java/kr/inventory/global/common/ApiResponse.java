package kr.inventory.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String status,       // success | error
        String code,         // OK 또는 비즈니스 에러 코드
        String message,      // 성공 시 null, 실패 시 에러 메시지
        String path,         // 요청 경로
        OffsetDateTime timestamp,
        T data
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static <T> ApiResponse<T> success(T data, String path) {
        return new ApiResponse<>(
                "success",
                "OK",
                null,
                path,
                OffsetDateTime.now(KST),
                data
        );
    }

    public static ApiResponse<Void> success(String path) {
        return new ApiResponse<>(
                "success",
                "OK",
                null,
                path,
                OffsetDateTime.now(KST),
                null
        );
    }

    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return new ApiResponse<>(
                "error",
                code,
                message,
                path,
                OffsetDateTime.now(KST),
                null
        );
    }
}
