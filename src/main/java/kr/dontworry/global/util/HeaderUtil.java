package kr.dontworry.global.util;

import kr.dontworry.global.auth.constant.AuthConstant;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class HeaderUtil {
    public void setAccessTokenHeader(HttpHeaders headers, String accessToken) {
        headers.set(AuthConstant.AUTHORIZATION_HEADER,
                AuthConstant.BEARER_PREFIX + accessToken);
    }
}
