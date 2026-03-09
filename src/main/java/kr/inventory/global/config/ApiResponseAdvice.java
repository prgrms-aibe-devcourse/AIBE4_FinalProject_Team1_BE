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


@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        if (returnType.getParameterType().equals(ApiResponse.class)) {
            return false;
        }

        if (returnType.getParameterType().equals(String.class)) {
            return false;
        }

        if (returnType.getParameterType().equals(byte[].class)) {
            return false;
        }

        if (converterType.getName().contains("ByteArrayHttpMessageConverter")) {
            return false;
        }

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

        if (path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/webjars/")) {
            return body;
        }

        if (path.contains("/auth/login") ||
            path.contains("/auth/kakao") ||
            path.contains("/auth/reissue")) {
            return body;
        }

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
