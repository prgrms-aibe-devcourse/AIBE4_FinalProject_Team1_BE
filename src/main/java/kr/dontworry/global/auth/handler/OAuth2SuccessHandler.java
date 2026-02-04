package kr.dontworry.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.dontworry.domain.auth.security.CustomUserDetails;
import kr.dontworry.domain.auth.model.RefreshToken;
import kr.dontworry.domain.auth.repository.RefreshTokenRepository;
import kr.dontworry.global.auth.constant.AuthConstant;
import kr.dontworry.global.auth.jwt.JwtProvider;
import kr.dontworry.global.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CookieUtil cookieUtil;

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

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path(AuthConstant.OAUTH_REDIRECT_PATH)
                .queryParam(AuthConstant.REDIRECT_PARAM_CODE, temporaryCode)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}