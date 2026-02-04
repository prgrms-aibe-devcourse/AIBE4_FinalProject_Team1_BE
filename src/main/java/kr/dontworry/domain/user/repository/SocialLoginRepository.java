package kr.dontworry.domain.user.repository;

import kr.dontworry.domain.user.entity.SocialLogin;
import kr.dontworry.domain.user.entity.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {
    Optional<SocialLogin> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
