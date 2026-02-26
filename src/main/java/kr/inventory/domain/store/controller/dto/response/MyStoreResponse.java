package kr.inventory.domain.store.controller.dto.response;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.global.util.BusinessRegistrationNumberUtil;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MyStoreResponse(
    Long storeId,
    UUID storePublicId,
    String storeName,
    String businessRegistrationNumber,
    StoreMemberRole myRole,
    StoreMemberStatus memberStatus,
    OffsetDateTime storeCreatedAt
) {
    public static MyStoreResponse from(StoreMember member) {
        Store store = member.getStore();
        return new MyStoreResponse(
            store.getStoreId(),
            store.getStorePublicId(),
            store.getName(),
            BusinessRegistrationNumberUtil.format(store.getBusinessRegistrationNumber()),
            member.getRole(),
            member.getStatus(),
            store.getCreatedAt()
        );
    }
}
