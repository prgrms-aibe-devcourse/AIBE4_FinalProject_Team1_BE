package kr.dontworry.auth.service;

import kr.dontworry.auth.entity.RefreshToken;
import kr.dontworry.auth.repository.RefreshTokenRepository;
import kr.dontworry.global.auth.jwt.JwtProvider;
import kr.dontworry.global.auth.oauth2.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Authentication authentication = jwtProvider.getAuthentication(refreshToken);
        Long userId = extractUserId(authentication);

        RefreshToken savedToken = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Refresh token not found in storage"));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            refreshTokenRepository.delete(savedToken);
            throw new RuntimeException("Refresh token mismatch");
        }

        return jwtProvider.createAccessToken(authentication, userId);
    }

    private Long extractUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUserId();
    }
}