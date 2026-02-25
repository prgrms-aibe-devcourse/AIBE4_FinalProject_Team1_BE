package kr.inventory.domain.store.controller.dto.response;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;

import java.util.UUID;

public record InvitationAcceptResponse(
    Long storeId,
    UUID storePublicId,
    String storeName,
    String role,
    String status
) {
    public static InvitationAcceptResponse from(StoreMember member) {
        Store store = member.getStore();
        return new InvitationAcceptResponse(
            store.getStoreId(),
            store.getStorePublicId(),
            store.getName(),
            member.getRole().name(),
            member.getStatus().name()
        );
    }
}
