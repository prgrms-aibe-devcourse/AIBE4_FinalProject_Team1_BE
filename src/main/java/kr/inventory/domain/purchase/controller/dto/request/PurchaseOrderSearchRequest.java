package kr.inventory.domain.purchase.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;

public record PurchaseOrderSearchRequest(
        @Schema(description = "발주 상태 필터 (ORDERED | CANCELED)", example = "ORDERED") PurchaseOrderStatus status,

        @Schema(description = "검색어 (주문번호 또는 거래처명)", example = "PO-2026") String search) {
}
