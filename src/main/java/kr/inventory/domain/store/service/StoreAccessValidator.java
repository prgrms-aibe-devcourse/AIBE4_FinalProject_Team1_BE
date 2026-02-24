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

        // ACTIVE 상태인지 확인 (선택적)
        // if (member.getStatus() != StoreMemberStatus.ACTIVE) {
        //     throw new StoreException(StoreErrorCode.NOT_STORE_MEMBER);
        // }

        return storeId;
    }

    /**
     * 사용자가 매장 멤버인지 검증만 수행
     *
     * @param userId 사용자 ID
     * @param storeId 매장 ID
     * @throws StoreException 매장 멤버가 아닌 경우
     */
    public void validateStoreMember(Long userId, Long storeId) {
        boolean isMember = storeMemberRepository.isStoreMember(storeId, userId);
        if (!isMember) {
            throw new StoreException(StoreErrorCode.NOT_STORE_MEMBER);
        }
    }
}
