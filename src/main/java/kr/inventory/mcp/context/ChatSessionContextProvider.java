package kr.inventory.mcp.context;

import jakarta.servlet.http.HttpServletRequest;
import kr.inventory.domain.auth.security.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class ChatSessionContextProvider {

    private static final String STORE_PUBLIC_ID_HEADER = "X-Store-Public-Id";

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException("인증 정보에서 사용자 정보를 찾을 수 없습니다.");
        }

        return userDetails.getUserId();
    }

    public UUID getCurrentStorePublicId() {
        HttpServletRequest request = getCurrentHttpRequest();
        String rawStorePublicId = request.getHeader(STORE_PUBLIC_ID_HEADER);

        if (rawStorePublicId == null || rawStorePublicId.isBlank()) {
            throw new IllegalStateException("요청 헤더에 매장 정보가 없습니다. X-Store-Public-Id 헤더를 확인하세요.");
        }

        try {
            return UUID.fromString(rawStorePublicId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("유효하지 않은 storePublicId 형식입니다: " + rawStorePublicId, e);
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            throw new IllegalStateException("현재 HTTP 요청 컨텍스트를 찾을 수 없습니다.");
        }

        return servletRequestAttributes.getRequest();
    }
}