package kr.dontworry.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import kr.dontworry.domain.auth.dto.TokenResponse;
import kr.dontworry.domain.auth.service.AuthService;
import kr.dontworry.global.auth.constant.AuthConstant;
import kr.dontworry.global.util.CookieUtil;
import kr.dontworry.global.util.HeaderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auths")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final HeaderUtil headerUtil;

    @PostMapping("/tokens")
    public ResponseEntity<Void> reissueTokens(
            @CookieValue(name = AuthConstant.REFRESH_TOKEN_COOKIE_NAME) String refreshToken, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.reissueTokens(refreshToken);

        cookieUtil.addRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.noContent()
                .headers(headerUtil.createAccessTokenHeaders(tokenResponse.accessToken()))
                .build();
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestParam String code) {
        String accessToken = authService.loginWithCode(code);

        return ResponseEntity.ok()
                .headers(headerUtil.createAccessTokenHeaders(accessToken))
                .body("Login Success");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(AuthConstant.AUTHORIZATION_HEADER) String bearerToken,
            HttpServletResponse response) {

        authService.logout(headerUtil.extractToken(bearerToken));

        cookieUtil.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok().build();
    }
}