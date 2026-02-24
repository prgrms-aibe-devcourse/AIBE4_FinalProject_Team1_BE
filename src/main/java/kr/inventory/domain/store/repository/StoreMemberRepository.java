package kr.inventory.domain.store.repository;

import kr.inventory.domain.store.entity.StoreMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long>, StoreMemberRepositoryCustom {

    Optional<StoreMember> findByStoreStoreIdAndUserUserId(Long storeId, Long userId);
}
