package kr.inventory.domain.store.controller.dto.response;

import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.user.entity.User;

public record StoreMemberResponse(
    Long storeMemberId,
    Long userId,
    String userName,
    String userEmail,
    StoreMemberRole role,
    StoreMemberStatus status
) {
    public static StoreMemberResponse from(StoreMember member) {
        User user = member.getUser();
        return new StoreMemberResponse(
            member.getStoreMemberId(),
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            member.getRole(),
            member.getStatus()
        );
    }
}
