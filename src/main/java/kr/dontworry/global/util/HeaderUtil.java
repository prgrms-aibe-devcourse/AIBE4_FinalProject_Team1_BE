package kr.dontworry.global.util;

import kr.dontworry.domain.auth.exception.AuthErrorCode;
import kr.dontworry.domain.auth.exception.AuthException;
import kr.dontworry.global.auth.constant.AuthConstant;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HeaderUtil {
    public void setAccessTokenHeader(HttpHeaders headers, String accessToken) {
        headers.set(AuthConstant.AUTHORIZATION_HEADER,
                AuthConstant.BEARER_PREFIX + accessToken);
    }

    public HttpHeaders createAccessTokenHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        setAccessTokenHeader(headers, accessToken);
        return headers;
    }

    public String extractToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConstant.BEARER_PREFIX)) {
            return bearerToken.substring(AuthConstant.BEARER_PREFIX.length());
        }

        throw new AuthException(AuthErrorCode.INVALID_TOKEN_FORMAT);
    }
}
