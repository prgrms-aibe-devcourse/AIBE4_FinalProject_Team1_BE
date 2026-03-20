package kr.inventory.global.auth.interceptor;

import java.util.Map;
import kr.inventory.global.auth.jwt.JwtProvider;
import kr.inventory.global.constant.WebSocketConstants;
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

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        log.debug("[StompInterceptor] Received STOMP message - command: {}, destination: {}, sessionId: {}",
                accessor.getCommand(), accessor.getDestination(), accessor.getSessionId());

        if (!requiresAuthentication(accessor.getCommand())) {
            return message;
        }

        if (accessor.getUser() != null) {
            return message;
        }

        Authentication sessionAuthentication = resolveSessionAuthentication(accessor);
        if (sessionAuthentication != null) {
            accessor.setUser(sessionAuthentication);
            return message;
        }

        String accessToken = resolveToken(accessor);
        if (!StringUtils.hasText(accessToken)) {
            log.warn("[StompInterceptor] Rejecting STOMP frame without access token - command: {}, sessionId: {}",
                    accessor.getCommand(), accessor.getSessionId());
            return null;
        }

        if (!jwtProvider.validateToken(accessToken)) {
            log.warn(
                    "Ignoring STOMP frame with invalid token. sessionId={}, command={}",
                    accessor.getSessionId(),
                    accessor.getCommand()
            );
            return null;
        }

        String jti = jwtProvider.getJti(accessToken);
        String isLogout = redisTemplate.opsForValue().get(
                kr.inventory.domain.auth.constant.AuthConstant.BLACKLIST_PREFIX + jti
        );

        if (!ObjectUtils.isEmpty(isLogout)) {
            log.warn(
                    "Ignoring STOMP frame with blacklisted token. sessionId={}, command={}",
                    accessor.getSessionId(),
                    accessor.getCommand()
            );
            return null;
        }

        Authentication authentication = jwtProvider.getAuthentication(accessToken);
        accessor.setUser(authentication);
        storeSessionAuthentication(accessor, authentication);
        return message;
    }

    private boolean requiresAuthentication(StompCommand command) {
        return StompCommand.CONNECT.equals(command)
                || StompCommand.SEND.equals(command)
                || StompCommand.SUBSCRIBE.equals(command);
    }

    private Authentication resolveSessionAuthentication(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null || sessionAttributes.isEmpty()) {
            return null;
        }

        Object value = sessionAttributes.get(WebSocketConstants.SESSION_AUTHENTICATION);
        if (value instanceof Authentication authentication) {
            return authentication;
        }

        return null;
    }

    private void storeSessionAuthentication(StompHeaderAccessor accessor, Authentication authentication) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }

        sessionAttributes.put(WebSocketConstants.SESSION_AUTHENTICATION, authentication);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = firstNativeHeader(
                accessor,
                WebSocketConstants.AUTHORIZATION_HEADER,
                WebSocketConstants.AUTHORIZATION_HEADER_LOWER_CASE
        );

        if (StringUtils.hasText(authorization)) {
            return stripBearerPrefix(authorization);
        }

        String token = firstNativeHeader(accessor, WebSocketConstants.TOKEN_HEADER, WebSocketConstants.ACCESS_TOKEN_HEADER);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null || sessionAttributes.isEmpty()) {
            return null;
        }

        Object sessionAuthorization = sessionAttributes.get(WebSocketConstants.SESSION_AUTHORIZATION);
        if (sessionAuthorization instanceof String value && StringUtils.hasText(value)) {
            return stripBearerPrefix(value);
        }

        Object sessionToken = sessionAttributes.get(WebSocketConstants.SESSION_ACCESS_TOKEN);
        if (sessionToken instanceof String value && StringUtils.hasText(value)) {
            return value.trim();
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

    private String stripBearerPrefix(String rawValue) {
        String value = rawValue.trim();
        if (value.startsWith(WebSocketConstants.BEARER_PREFIX)) {
            return value.substring(WebSocketConstants.BEARER_PREFIX.length());
        }
        return value;
    }
}