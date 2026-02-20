package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PurchaseOrderSummaryResponse(
        Long purchaseOrderId,
        Long storeId,
        String orderNo,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        OffsetDateTime submittedAt
) {
}
