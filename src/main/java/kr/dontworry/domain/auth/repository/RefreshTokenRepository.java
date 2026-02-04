package kr.dontworry.domain.auth.repository;

import kr.dontworry.domain.auth.model.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}