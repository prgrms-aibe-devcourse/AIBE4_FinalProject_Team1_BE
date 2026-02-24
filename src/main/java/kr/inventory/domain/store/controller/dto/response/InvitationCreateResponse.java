package kr.inventory.domain.store.controller.dto.response;

import kr.inventory.domain.store.entity.Invitation;

import java.time.OffsetDateTime;

public record InvitationCreateResponse(
    Long invitationId,
    Long storeId,
    String inviteUrl,
    String inviteCode,
    OffsetDateTime expiresAt
) {
    public static InvitationCreateResponse from(Invitation invitation, String frontBaseUrl) {
        String inviteUrl = frontBaseUrl + "/invite?token=" + invitation.getToken();
        return new InvitationCreateResponse(
            invitation.getInvitationId(),
            invitation.getStore().getStoreId(),
            inviteUrl,
            invitation.getCode(),
            invitation.getExpiresAt()
        );
    }
}
