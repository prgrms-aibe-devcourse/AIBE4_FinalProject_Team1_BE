package kr.inventory.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.inventory.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.error(
                "FORBIDDEN",
                "접근 권한이 없습니다.",
                getRequestPath(request)
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String getRequestPath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String requestURI = request.getRequestURI();
        return queryString != null ? requestURI + "?" + queryString : requestURI;
    }
}