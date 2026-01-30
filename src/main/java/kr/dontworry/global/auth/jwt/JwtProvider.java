package kr.dontworry.global.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import kr.dontworry.domain.auth.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-exp}")
    private long accessTokenExpTime;

    @Value("${jwt.refresh-exp}")
    private long refreshTokenExpTime;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Authentication auth, Long userId) {
        String authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("auth", authorities)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId, String jti) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(jti)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        Long userId = getUserId(token);

        CustomUserDetails principal =
                new CustomUserDetails(userId, authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.info("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}