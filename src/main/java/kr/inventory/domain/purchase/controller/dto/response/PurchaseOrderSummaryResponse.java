package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseOrderSummaryResponse(
        Long purchaseOrderId,
        Long storeId,
        UUID vendorPublicId,
        String vendorName,
        String orderNo,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        OffsetDateTime submittedAt
) {
}
