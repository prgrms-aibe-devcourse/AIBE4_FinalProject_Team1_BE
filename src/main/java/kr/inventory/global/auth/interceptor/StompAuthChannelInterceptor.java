package kr.inventory.global.auth.interceptor;

import kr.inventory.domain.auth.constant.AuthConstant;
import kr.inventory.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_HEADER_LOWER_CASE = "authorization";
    private static final String TOKEN_HEADER = "token";
    private static final String ACCESS_TOKEN_HEADER = "accessToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (!requiresAuthentication(command) || accessor.getUser() != null) {
            return message;
        }

        String accessToken = resolveToken(accessor);
        if (!StringUtils.hasText(accessToken)) {
            return message;
        }

        if (!jwtProvider.validateToken(accessToken)) {
            log.warn("Ignoring STOMP frame with invalid token. sessionId={}, command={}", accessor.getSessionId(), command);
            return message;
        }

        String jti = jwtProvider.getJti(accessToken);
        String isLogout = redisTemplate.opsForValue().get(AuthConstant.BLACKLIST_PREFIX + jti);

        if (!ObjectUtils.isEmpty(isLogout)) {
            log.warn("Ignoring STOMP frame with blacklisted token. sessionId={}, command={}", accessor.getSessionId(), command);
            return message;
        }

        Authentication authentication = jwtProvider.getAuthentication(accessToken);
        accessor.setUser(authentication);

        return message;
    }

    private boolean requiresAuthentication(StompCommand command) {
        return StompCommand.CONNECT.equals(command)
                || StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = firstNativeHeader(
                accessor,
                AUTHORIZATION_HEADER,
                AUTHORIZATION_HEADER_LOWER_CASE
        );

        if (StringUtils.hasText(authorization)) {
            String value = authorization.trim();
            if (value.startsWith(BEARER_PREFIX)) {
                return value.substring(BEARER_PREFIX.length());
            }
            return value;
        }

        String token = firstNativeHeader(accessor, TOKEN_HEADER, ACCESS_TOKEN_HEADER);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }

        return null;
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String... headerNames) {
        for (String headerName : headerNames) {
            String value = accessor.getFirstNativeHeader(headerName);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
