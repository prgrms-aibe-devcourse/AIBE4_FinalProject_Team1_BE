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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public TokenResponse reissueTokens(String refreshToken) {
        RefreshToken savedToken = validateAndGetStoredRefreshToken(refreshToken);

        Authentication authentication = createAuthentication(savedToken.getUserId());

        return rotateTokens(savedToken, authentication);
    }

    private RefreshToken validateAndGetStoredRefreshToken( String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String sid = jwtProvider.getSid(refreshToken);
        Long userId = jwtProvider.getUserId(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findById(sid)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!savedToken.getUserId().equals(userId)) {
            refreshTokenRepository.delete(savedToken);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        return savedToken;
    }

    private Authentication createAuthentication(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(UserErrorCode.USER_NOT_FOUND));

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }

    private TokenResponse rotateTokens(RefreshToken oldToken, Authentication authentication) {
        Long userId = oldToken.getUserId();

        refreshTokenRepository.delete(oldToken);

        String sid = UUID.randomUUID().toString();

        String newAccessToken = jwtProvider.createAccessToken(authentication, userId, sid);
        String newRefreshToken = jwtProvider.createRefreshToken(userId, sid);

        refreshTokenRepository.save(new RefreshToken(sid, userId));

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String accessToken){
        if(!jwtProvider.validateToken(accessToken)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        String sid = jwtProvider.getSid(accessToken);
        refreshTokenRepository.deleteById(sid);

        String jti = jwtProvider.getJti(accessToken);
        Long expiration = jwtProvider.getExpiration(accessToken);
        redisTemplate.opsForValue().set(
                "BL:" + jti,
                "logout",
                expiration,
                TimeUnit.MILLISECONDS
        );
    }

    public String loginWithCode(String code) {
        String accessToken = redisTemplate.opsForValue().get(code);

        if (accessToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_AUTH_CODE);
        }

        redisTemplate.delete(code);

        return accessToken;
    }
}