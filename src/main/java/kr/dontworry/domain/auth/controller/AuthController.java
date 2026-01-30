package kr.dontworry.domain.auth.controller;

import kr.dontworry.domain.auth.dto.AccessTokenResponse;
import kr.dontworry.domain.auth.service.AuthService;
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

    @PostMapping("/access-tokens")
    public ResponseEntity<AccessTokenResponse> reissueAccessToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccessToken = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(new AccessTokenResponse(newAccessToken));
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