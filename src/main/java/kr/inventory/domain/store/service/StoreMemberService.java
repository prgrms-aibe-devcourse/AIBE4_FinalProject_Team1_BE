package kr.inventory.domain.store.service;

import kr.inventory.domain.store.controller.dto.request.*;
import kr.inventory.domain.store.controller.dto.response.*;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreMemberRepository;
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

    public List<StoreMemberResponse> getStoreMembers(UUID storePublicId) {
        List<StoreMember> members = storeMemberRepository.findAllByStorePublicIdWithUser(storePublicId);

        return members.stream()
            .map(StoreMemberResponse::from)
            .toList();
    }

    public StoreMemberResponse getStoreMember(UUID storePublicId, Long memberId) {
        StoreMember member = storeMemberRepository.findById(memberId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.MEMBER_NOT_FOUND));

        if (!member.getStore().getStorePublicId().equals(storePublicId)) {
            throw new StoreException(StoreErrorCode.MEMBER_NOT_FOUND_IN_STORE);
        }

        return StoreMemberResponse.from(member);
    }

    @Transactional
    public StoreMemberResponse updateMemberStatus(UUID storePublicId, Long memberId, MemberStatusUpdateRequest request) {
        StoreMember targetMember = storeMemberRepository.findById(memberId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.MEMBER_NOT_FOUND));

        if (!targetMember.getStore().getStorePublicId().equals(storePublicId)) {
            throw new StoreException(StoreErrorCode.MEMBER_NOT_FOUND_IN_STORE);
        }

        if (targetMember.getRole() == StoreMemberRole.OWNER &&
            request.status() == StoreMemberStatus.INACTIVE) {
            throw new StoreException(StoreErrorCode.CANNOT_DEACTIVATE_OWNER);
        }

        StoreMemberStatus previousStatus = targetMember.getStatus();
        targetMember.updateStatus(request.status());

        if (request.status() == StoreMemberStatus.INACTIVE) {
            // INACTIVE로 변경 시 대표 매장 해제
            targetMember.unsetAsDefault();
        } else if (request.status() == StoreMemberStatus.ACTIVE && previousStatus == StoreMemberStatus.INACTIVE) {
            // INACTIVE → ACTIVE 재활성화 시, ACTIVE 매장이 1개뿐이면 대표 매장으로 설정
            Long userId = targetMember.getUser().getUserId();
            long activeStoreCount = storeMemberRepository.countActiveByUserId(userId);
            if (activeStoreCount == 1) {
                storeMemberRepository.unsetAllDefaultsByUserId(userId);
                targetMember.setAsDefault();
            }
        }

        return StoreMemberResponse.from(targetMember);
    }
}
