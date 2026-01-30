package kr.dontworry.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 604800)
public class RefreshToken {

    @Id
    private String jti;

    private Long userId;
}