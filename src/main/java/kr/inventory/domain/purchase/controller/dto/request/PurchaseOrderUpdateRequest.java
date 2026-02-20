package kr.inventory.domain.purchase.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseOrderUpdateRequest(
        @Valid
        @Size(min = 1, message = "items는 최소 1개 이상이어야 합니다.")
        List<PurchaseOrderItemRequest> items
) {
}
