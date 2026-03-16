package kr.inventory.global.auth.interceptor;

import java.util.Map;
import kr.inventory.global.constant.WebSocketConstants;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        String authorization = firstText(queryParams, WebSocketConstants.AUTHORIZATION_HEADER, WebSocketConstants.AUTHORIZATION_HEADER_LOWER_CASE);
        if (StringUtils.hasText(authorization)) {
            attributes.put(WebSocketConstants.SESSION_AUTHORIZATION, authorization.trim());
        }

        String accessToken = firstText(queryParams, WebSocketConstants.TOKEN_PARAM, WebSocketConstants.ACCESS_TOKEN_PARAM);
        if (StringUtils.hasText(accessToken)) {
            attributes.put(WebSocketConstants.SESSION_ACCESS_TOKEN, accessToken.trim());
        }

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String firstText(MultiValueMap<String, String> source, String... keys) {
        for (String key : keys) {
            String value = source.getFirst(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
