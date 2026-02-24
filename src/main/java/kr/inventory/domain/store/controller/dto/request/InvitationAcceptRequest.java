package kr.inventory.domain.store.controller.dto.request;

public record InvitationAcceptRequest(
    String token,
    Long storeId,
    String code
) {
}
