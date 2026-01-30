package kr.dontworry.auth.service;

import kr.dontworry.auth.entity.RefreshToken;
import kr.dontworry.auth.exception.AuthException;
import kr.dontworry.auth.exception.ErrorCode;
import kr.dontworry.auth.repository.RefreshTokenRepository;
import kr.dontworry.global.auth.jwt.JwtProvider;
import kr.dontworry.auth.dto.CustomUserDetails;
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
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Authentication authentication = jwtProvider.getAuthentication(refreshToken);
        Long userId = extractUserId(authentication);

        RefreshToken savedToken = refreshTokenRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            refreshTokenRepository.delete(savedToken);
            throw new AuthException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        return jwtProvider.createAccessToken(authentication, userId);
    }

    private Long extractUserId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getUserId();
    }
}