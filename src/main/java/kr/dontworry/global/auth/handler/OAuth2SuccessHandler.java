package kr.dontworry.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.dontworry.domain.auth.dto.CustomOAuth2User;
import kr.dontworry.domain.auth.entity.RefreshToken;
import kr.dontworry.domain.auth.repository.RefreshTokenRepository;
import kr.dontworry.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend-url:http://localhost}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User principal =
                (CustomOAuth2User) authentication.getPrincipal();

        Long userId = principal.getUserId();

        String accessToken = jwtProvider.createAccessToken(authentication, userId);
        String refreshToken = jwtProvider.createRefreshToken(authentication, userId);

        refreshTokenRepository.save(new RefreshToken(userId, refreshToken));

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/oauth/redirect")
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}