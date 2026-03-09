package kr.inventory.global.config;

import jakarta.servlet.http.HttpServletRequest;
import kr.inventory.global.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 모든 API 응답을 ApiResponse 형식으로 자동 래핑.
 * - 이미 ApiResponse로 감싸진 경우 그대로 반환
 * - ResponseEntity를 반환하는 경우 제외 (인증 등)
 * - 에러 응답은 GlobalExceptionHandler에서 처리
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 이미 ApiResponse로 래핑된 경우 제외
        if (returnType.getParameterType().equals(ApiResponse.class)) {
            return false;
        }

        // String을 직접 반환하는 경우 제외 (StringHttpMessageConverter 충돌 방지)
        if (returnType.getParameterType().equals(String.class)) {
            return false;
        }

        // byte[]를 반환하는 경우 제외 (Swagger 리소스, 파일 다운로드 등)
        if (returnType.getParameterType().equals(byte[].class)) {
            return false;
        }

        // ByteArrayHttpMessageConverter인 경우 제외
        if (converterType.getName().contains("ByteArrayHttpMessageConverter")) {
            return false;
        }

        // StringHttpMessageConverter인 경우 제외
        if (converterType.getName().contains("StringHttpMessageConverter")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        // 이미 ApiResponse인 경우 그대로 반환
        if (body instanceof ApiResponse) {
            return body;
        }

        // byte[] 타입은 그대로 반환 (리소스 파일 등)
        if (body instanceof byte[]) {
            return body;
        }

        // String 타입은 그대로 반환
        if (body instanceof String) {
            return body;
        }

        // 요청 경로 추출
        String path = extractPath(request);

        // Swagger/OpenAPI 경로는 래핑하지 않음
        if (path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/webjars/")) {
            return body;
        }

        // 인증 관련 경로는 래핑하지 않음 (로그인, 토큰 재발급 등)
        if (path.contains("/auth/login") ||
            path.contains("/auth/kakao") ||
            path.contains("/auth/reissue")) {
            return body;
        }

        // 파일 다운로드 경로는 래핑하지 않음
        if (path.contains("/download") || path.endsWith(".pdf")) {
            return body;
        }

        // 성공 응답으로 래핑
        return ApiResponse.success(body, path);
    }

    private String extractPath(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String queryString = httpRequest.getQueryString();
            String requestURI = httpRequest.getRequestURI();
            return queryString != null ? requestURI + "?" + queryString : requestURI;
        }
        return request.getURI().getPath();
    }
}
