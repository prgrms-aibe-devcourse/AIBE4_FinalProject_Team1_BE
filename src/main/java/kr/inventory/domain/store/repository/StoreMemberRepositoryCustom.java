package kr.inventory.domain.store.repository;

import java.util.Optional;
import java.util.UUID;

public interface StoreMemberRepositoryCustom {
    Optional<Long> findStoreIdByUserAndPublicId(Long userId, UUID publicId);
}
