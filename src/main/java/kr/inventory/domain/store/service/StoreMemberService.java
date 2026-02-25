package kr.inventory.domain.store.service;

import kr.inventory.domain.store.controller.dto.request.*;
import kr.inventory.domain.store.controller.dto.response.*;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreMemberService {

    private final StoreMemberRepository storeMemberRepository;
    private final StoreRepository storeRepository;

    public List<StoreMemberResponse> getStoreMembers(Long userId, UUID storePublicId) {
        boolean isMember = storeMemberRepository.isStoreMemberByPublicId(storePublicId, userId);
        if (!isMember) {
            throw new StoreException(StoreErrorCode.NOT_STORE_MEMBER);
        }

        // 매장의 모든 멤버 조회
        List<StoreMember> members = storeMemberRepository.findAllByStorePublicIdWithUser(storePublicId);

        return members.stream()
            .map(StoreMemberResponse::from)
            .toList();
    }

    public StoreMemberResponse getStoreMember(Long userId, UUID storePublicId, Long memberId) {
        boolean isMember = storeMemberRepository.isStoreMemberByPublicId(storePublicId, userId);
        if (!isMember) {
            throw new StoreException(StoreErrorCode.NOT_STORE_MEMBER);
        }

        StoreMember member = storeMemberRepository.findById(memberId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.MEMBER_NOT_FOUND));

        // 매장 일치 여부 확인
        if (!member.getStore().getStorePublicId().equals(storePublicId)) {
            throw new StoreException(StoreErrorCode.MEMBER_NOT_FOUND_IN_STORE);
        }

        return StoreMemberResponse.from(member);
    }

    @Transactional
    public StoreMemberResponse updateMemberStatus(Long userId, UUID storePublicId, Long memberId, MemberStatusUpdateRequest request) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        // 권한 검증
        storeMemberRepository.findByStoreStoreIdAndUserUserId(store.getStoreId(), userId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.NOT_STORE_MEMBER));

        // 멤버 조회
        StoreMember targetMember = storeMemberRepository.findById(memberId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.MEMBER_NOT_FOUND));

        if (!targetMember.getStore().getStorePublicId().equals(storePublicId)) {
            throw new StoreException(StoreErrorCode.MEMBER_NOT_FOUND_IN_STORE);
        }

        // OWNER는 INACTIVE 금지
        if (targetMember.getRole() == StoreMemberRole.OWNER &&
            request.status() == StoreMemberStatus.INACTIVE) {
            throw new StoreException(StoreErrorCode.CANNOT_DEACTIVATE_OWNER);
        }

        targetMember.updateStatus(request.status());

        return StoreMemberResponse.from(targetMember);
    }
}
