package kr.inventory.domain.store.service;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 매장 접근 권한 검증 유틸리티
 */
@Component
@RequiredArgsConstructor
public class StoreAccessValidator {

    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;

    /**
     * 사용자의 매장 접근 권한을 검증하고 내부 Store ID를 반환
     *
     * @param userId 사용자 ID
     * @param storePublicId 매장 Public ID (UUID)
     * @return 내부 Store ID (Long)
     * @throws StoreException 매장을 찾을 수 없거나 접근 권한이 없는 경우
     */
    public Long validateAndGetStoreId(Long userId, UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Long storeId = store.getStoreId();

        StoreMember member = storeMemberRepository
            .findByStoreStoreIdAndUserUserId(storeId, userId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.NOT_STORE_MEMBER));

        return storeId;
    }
}
