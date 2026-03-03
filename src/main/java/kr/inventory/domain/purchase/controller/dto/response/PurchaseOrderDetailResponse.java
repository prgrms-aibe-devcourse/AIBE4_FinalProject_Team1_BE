package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderDetailResponse(
        UUID purchaseOrderPublicId,
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
    public static PurchaseOrderDetailResponse from(PurchaseOrder purchaseOrder) {
        Vendor vendor = purchaseOrder.getVendor();
        return new PurchaseOrderDetailResponse(
                purchaseOrder.getPurchaseOrderPublicId(),
                purchaseOrder.getStore().getStoreId(),
                vendor == null ? null : vendor.getVendorPublicId(),
                vendor == null ? null : vendor.getName(),
                purchaseOrder.getOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getSubmittedByUserId(),
                purchaseOrder.getSubmittedAt(),
                purchaseOrder.getConfirmedByUserId(),
                purchaseOrder.getConfirmedAt(),
                purchaseOrder.getCanceledByUserId(),
                purchaseOrder.getCanceledAt(),
                purchaseOrder.getItems().stream()
                        .map(PurchaseOrderItemResponse::from)
                        .toList()
        );
    }
}
