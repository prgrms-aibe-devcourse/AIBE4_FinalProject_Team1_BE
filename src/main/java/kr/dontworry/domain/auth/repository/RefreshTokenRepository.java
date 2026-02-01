package kr.dontworry.domain.auth.repository;

import kr.dontworry.domain.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    void deleteByUserId(Long userId);
}