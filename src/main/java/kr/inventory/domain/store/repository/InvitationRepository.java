package kr.inventory.domain.store.repository;

import kr.inventory.domain.store.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByStoreStoreId(Long storeId);

    Optional<Invitation> findByToken(String token);

    Optional<Invitation> findByStoreStoreIdAndCode(Long storeId, String code);
}
