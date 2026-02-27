package kr.inventory.domain.store.repository;

import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long>, StoreMemberRepositoryCustom {

    Optional<StoreMember> findByStoreStoreIdAndUserUserIdAndStatus(
            Long storeId,
            Long userId,
            StoreMemberStatus status
    );

    Optional<StoreMember> findByStoreStoreIdAndUserUserId(Long storeId, Long userId);
}
