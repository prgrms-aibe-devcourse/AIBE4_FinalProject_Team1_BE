package kr.inventory.global.config;

import jakarta.servlet.http.HttpServletRequest;
import kr.inventory.global.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        // 이미 String 컨버터가 선택된 경우는 절대 감싸면 안 됨
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }

        // 파일/바이너리 응답도 제외
        if (ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }

        if (ResourceHttpMessageConverter.class.isAssignableFrom(converterType)) {
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
        // 이미 공통 응답이면 그대로 반환
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        // 기술적으로 감싸면 안 되는 타입들
        if (body instanceof String || body instanceof byte[]) {
            return body;
        }

        if (body instanceof Resource || body instanceof StreamingResponseBody) {
            return body;
        }

        // JSON 계열이 아니면 감싸지 않음
        if (!isJsonLike(selectedContentType)) {
            return body;
        }

        // redirect, 204는 건드리지 않음
        if (isRedirectOrNoContent(response)) {
            return body;
        }

        String path = extractPath(request);

        // body가 없는 200 응답 처리
        if (body == null) {
            return ApiResponse.success(path);
        }

        return ApiResponse.success(body, path);
    }

    private boolean isJsonLike(MediaType contentType) {
        if (contentType == null) {
            return false;
        }

        return MediaType.APPLICATION_JSON.includes(contentType)
                || MediaType.APPLICATION_PROBLEM_JSON.includes(contentType)
                || contentType.getSubtype().endsWith("+json");
    }

    private boolean isRedirectOrNoContent(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            int status = servletResponse.getServletResponse().getStatus();
            return status == HttpStatus.NO_CONTENT.value() || (status >= 300 && status < 400);
        }
        return false;
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