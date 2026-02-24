package kr.inventory.domain.store.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;

public record MemberStatusUpdateRequest(
    @NotNull(message = "상태는 필수입니다")
    StoreMemberStatus status
) {
}
