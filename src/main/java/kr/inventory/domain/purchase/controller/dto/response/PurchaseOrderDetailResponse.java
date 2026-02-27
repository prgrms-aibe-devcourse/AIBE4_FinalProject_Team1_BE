package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderDetailResponse(
        Long purchaseOrderId,
        Long storeId,
        UUID vendorPublicId,
        String vendorName,
        String orderNo,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        Long submittedByUserId,
        OffsetDateTime submittedAt,
        Long confirmedByUserId,
        OffsetDateTime confirmedAt,
        Long canceledByUserId,
        OffsetDateTime canceledAt,
        List<PurchaseOrderItemResponse> items
) {
}
