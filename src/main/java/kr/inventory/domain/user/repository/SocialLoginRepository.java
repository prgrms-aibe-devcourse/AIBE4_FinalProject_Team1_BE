package kr.inventory.domain.user.repository;

import kr.inventory.domain.user.entity.SocialLogin;
import kr.inventory.domain.user.entity.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLoginRepository extends JpaRepository<SocialLogin, Long> {
    Optional<SocialLogin> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
