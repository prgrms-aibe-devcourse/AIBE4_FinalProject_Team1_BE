package kr.dontworry.domain.auth.service;

import kr.dontworry.domain.auth.dto.CustomUserDetails;
import kr.dontworry.domain.auth.dto.TokenResponse;
import kr.dontworry.domain.auth.entity.RefreshToken;
import kr.dontworry.domain.auth.exception.AuthErrorCode;
import kr.dontworry.domain.auth.exception.AuthException;
import kr.dontworry.domain.auth.repository.RefreshTokenRepository;
import kr.dontworry.domain.user.entity.User;
import kr.dontworry.domain.user.exception.UserErrorCode;
import kr.dontworry.domain.user.repository.UserRepository;
import kr.dontworry.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public TokenResponse refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String jti = jwtProvider.getJti(refreshToken);
        RefreshToken savedToken = refreshTokenRepository.findById(jti)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        Long userId = jwtProvider.getUserId(refreshToken);

        if (!savedToken.getUserId().equals(userId)) {
            refreshTokenRepository.delete(savedToken);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(UserErrorCode.USER_NOT_FOUND));

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        refreshTokenRepository.delete(savedToken);

        String newAccessToken = jwtProvider.createAccessToken(authentication, userId);

        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtProvider.createRefreshToken(userId, newJti);

        refreshTokenRepository.save(new RefreshToken(newJti, userId));

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}