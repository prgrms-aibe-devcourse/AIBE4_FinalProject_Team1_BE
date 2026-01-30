package kr.dontworry.domain.auth.controller;

import kr.dontworry.domain.auth.dto.AccessTokenResponse;
import kr.dontworry.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auths")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/access-tokens")
    public ResponseEntity<AccessTokenResponse> reissueAccessToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccessToken = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(new AccessTokenResponse(newAccessToken));
    }
}