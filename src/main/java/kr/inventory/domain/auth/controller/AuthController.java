package kr.inventory.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import kr.inventory.domain.auth.controller.dto.TokenResponse;
import kr.inventory.domain.auth.service.AuthService;
import kr.inventory.global.auth.constant.AuthConstant;
import kr.inventory.global.util.CookieUtil;
import kr.inventory.global.util.HeaderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증(Auth)", description = "인증관련 기능을 담당하는 API입니다.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final HeaderUtil headerUtil;

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 Access Token과 Refresh Token을 모두 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<Void> reissueTokens(
            @CookieValue(name = AuthConstant.REFRESH_TOKEN_COOKIE_NAME) String refreshToken, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.reissueTokens(refreshToken);

        cookieUtil.addRefreshTokenCookie(response, tokenResponse.refreshToken());

        return ResponseEntity.noContent()
                .headers(headerUtil.createAccessTokenHeaders(tokenResponse.accessToken()))
                .build();
    }

    @Operation(summary = "소셜 로그인", description = "인가 코드를 이용하여 로그인을 진행하고 Access Token을 발급합니다.")
    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestParam String code) {
        String accessToken = authService.loginWithCode(code);

        return ResponseEntity.ok()
                .headers(headerUtil.createAccessTokenHeaders(accessToken))
                .body("로그인에 성공했습니다.");
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(AuthConstant.AUTHORIZATION_HEADER) String bearerToken,
            @CookieValue(name = AuthConstant.REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            HttpServletResponse response) {

        authService.logout(headerUtil.extractToken(bearerToken), refreshToken);

        cookieUtil.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok().build();
    }
}