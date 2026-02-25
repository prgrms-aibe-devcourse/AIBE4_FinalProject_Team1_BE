package kr.inventory.domain.store.controller.dto.response;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.global.util.BusinessRegistrationNumberUtil;

import java.util.UUID;

public record StoreCreateResponse(
    Long storeId,
    UUID storePublicId,
    String name,
    String businessRegistrationNumber,
    StoreMemberRole myRole
) {
    public static StoreCreateResponse from(Store store, StoreMemberRole role) {
        return new StoreCreateResponse(
            store.getStoreId(),
            store.getStorePublicId(),
            store.getName(),
            BusinessRegistrationNumberUtil.format(store.getBusinessRegistrationNumber()),
            role
        );
    }
}
