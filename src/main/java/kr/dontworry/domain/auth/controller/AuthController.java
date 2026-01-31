package kr.dontworry.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import kr.dontworry.domain.auth.dto.TokenResponse;
import kr.dontworry.domain.auth.service.AuthService;
import kr.dontworry.global.auth.constant.AuthConstant;
import kr.dontworry.global.util.CookieUtil;
import kr.dontworry.global.util.HeaderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auths")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RedisTemplate<String, String> redisTemplate;
    private final CookieUtil cookieUtil;
    private final HeaderUtil headerUtil;

    @PostMapping("/tokens")
    public ResponseEntity<Void> reissueTokens(
            @CookieValue(name = AuthConstant.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken, HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenResponse tokenResponse = authService.reissueTokens(refreshToken);

        cookieUtil.addRefreshTokenCookie(response, tokenResponse.refreshToken());

        HttpHeaders headers = new HttpHeaders();
        headerUtil.setAccessTokenHeader(headers, tokenResponse.accessToken());

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestParam String code) {
        String accessToken = redisTemplate.opsForValue().get(code);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Code");
        }

        redisTemplate.delete(code);

        HttpHeaders headers = new HttpHeaders();
        headerUtil.setAccessTokenHeader(headers, accessToken);

        return ResponseEntity.ok()
                .headers(headers)
                .body("Login Success");
    }
}