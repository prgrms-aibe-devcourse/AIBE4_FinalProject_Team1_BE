package kr.inventory.domain.store.repository;

import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreMemberRepositoryCustom {
    Optional<Long> findStoreIdByUserAndPublicId(Long userId, UUID publicId);

    List<StoreMember> findAllByUserUserIdWithStore(Long userId);

    List<StoreMember> findAllByStoreStoreIdWithUser(Long storeId);

    boolean isStoreMember(Long storeId, Long userId);

    boolean hasRole(Long storeId, Long userId, StoreMemberRole role);
}
