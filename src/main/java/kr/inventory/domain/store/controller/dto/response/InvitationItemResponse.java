package kr.inventory.domain.store.controller.dto.response;

import kr.inventory.domain.store.entity.Invitation;
import kr.inventory.domain.store.entity.enums.InvitationStatus;
import kr.inventory.domain.user.entity.User;

import java.time.OffsetDateTime;

public record InvitationItemResponse(
    Long invitationId,
    String inviteUrl,
    String code,
    InvitationStatus status,
    String invitedByUserName,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt
) {
    public static InvitationItemResponse from(Invitation invitation, String frontBaseUrl) {
        User invitedBy = invitation.getInvitedBy();
        String inviteUrl = frontBaseUrl + "/invite?token=" + invitation.getToken();
        return new InvitationItemResponse(
            invitation.getInvitationId(),
            inviteUrl,
            invitation.getCode(),
            invitation.getStatus(),
            invitedBy.getName(),
            invitation.getExpiresAt(),
            invitation.getCreatedAt()
        );
    }
}
