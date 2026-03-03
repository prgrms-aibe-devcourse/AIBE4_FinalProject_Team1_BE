package kr.inventory.domain.purchase.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PurchaseOrderUpdateRequest(
        @NotNull(message = "vendorPublicId는 필수입니다.")
        UUID vendorPublicId,
        @Valid
        @Size(min = 1, message = "items는 최소 1개 이상이어야 합니다.")
        List<PurchaseOrderItemRequest> items
) {
}
