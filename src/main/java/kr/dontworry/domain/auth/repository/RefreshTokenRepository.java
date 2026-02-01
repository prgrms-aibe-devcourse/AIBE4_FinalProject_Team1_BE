package kr.dontworry.domain.auth.repository;

import kr.dontworry.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    void deleteByUserId(Long userId);
    Optional<RefreshToken> findByUserId(Long userId);
}