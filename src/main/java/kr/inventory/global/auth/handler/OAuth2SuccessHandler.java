package kr.inventory.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.inventory.domain.auth.model.RefreshToken;
import kr.inventory.domain.auth.repository.RefreshTokenRepository;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.global.auth.constant.AuthConstant;
import kr.inventory.global.auth.jwt.JwtProvider;
import kr.inventory.global.config.CorsProperties;
import kr.inventory.global.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CookieUtil cookieUtil;
    private final CorsProperties corsProperties;

    @Value("${app.frontend-url:http://localhost}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUserDetails principal =
                (CustomUserDetails) authentication.getPrincipal();
        Long userId = principal.getUserId();

        String sid = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        String accessToken = jwtProvider.createAccessToken(authentication, userId);
        String refreshToken = jwtProvider.createRefreshToken(userId, jti, sid);

        refreshTokenRepository.save(new RefreshToken(sid, jti, userId));
        cookieUtil.addRefreshTokenCookie(response, refreshToken);

        String temporaryCode = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(temporaryCode, accessToken, Duration.ofMinutes(1));

        String redirectUri = resolveRedirectUri(request);
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam(AuthConstant.REDIRECT_PARAM_CODE, temporaryCode)
                .build()
                .toUriString();

        cookieUtil.deleteCookie(response, AuthConstant.OAUTH_REDIRECT_URI_COOKIE_NAME);
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveRedirectUri(HttpServletRequest request) {
        Optional<String> redirectUriCookie = cookieUtil.getCookieValue(request, AuthConstant.OAUTH_REDIRECT_URI_COOKIE_NAME);

        if (redirectUriCookie.isPresent() && isAllowedRedirectUri(redirectUriCookie.get())) {
            return redirectUriCookie.get();
        }

        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(AuthConstant.OAUTH_REDIRECT_PATH)
                .build()
                .toUriString();
    }

    private boolean isAllowedRedirectUri(String redirectUri) {
        try {
            URI requestedUri = URI.create(redirectUri);

            if (requestedUri.getScheme() == null || requestedUri.getHost() == null) {
                return false;
            }

            String requestedOrigin = normalizeOrigin(requestedUri);
            List<String> allowedOrigins = corsProperties.getAllowedOrigins();

            return allowedOrigins.stream()
                    .map(this::normalizeOrigin)
                    .anyMatch(requestedOrigin::equals);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeOrigin(String origin) {
        return normalizeOrigin(URI.create(origin));
    }

    private String normalizeOrigin(URI uri) {
        String scheme = Optional.ofNullable(uri.getScheme())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        String host = Optional.ofNullable(uri.getHost())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        int port = uri.getPort();

        boolean isDefaultPort = port == -1
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);

        if (isDefaultPort) {
            return scheme + "://" + host;
        }

        return scheme + "://" + host + ":" + port;
    }
}