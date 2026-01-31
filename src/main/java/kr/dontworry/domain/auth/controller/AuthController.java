package kr.dontworry.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import kr.dontworry.domain.auth.dto.AccessTokenResponse;
import kr.dontworry.domain.auth.dto.TokenResponse;
import kr.dontworry.domain.auth.service.AuthService;
import kr.dontworry.global.auth.constant.AuthConstant;
import kr.dontworry.global.util.CookieUtil;
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

    @PostMapping("/access-tokens")
    public ResponseEntity<AccessTokenResponse> reissueAccessToken(
            @CookieValue(name = AuthConstant.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken, HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);

        response.setHeader("Authorization", "Bearer " + tokenResponse.accessToken());

        cookieUtil.addRefreshTokenCookie(response, tokenResponse.accessToken());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestParam String code) {
        String accessToken = redisTemplate.opsForValue().get(code);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Code");
        }

        redisTemplate.delete(code);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        return ResponseEntity.ok().headers(headers).body("Login Success");
    }
}