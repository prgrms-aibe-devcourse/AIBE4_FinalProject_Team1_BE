package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.reference.entity.Vendor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderDetailResponse(
        UUID purchaseOrderPublicId,
        UUID vendorPublicId,
        String vendorName,
        String orderNo,
        PurchaseOrderStatus status,
        BigDecimal totalAmount,
        UUID canceledByUserPublicId,
        OffsetDateTime canceledAt,
        List<PurchaseOrderItemResponse> items) {
    public static PurchaseOrderDetailResponse from(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> items, UUID canceledByUserPublicId) {
        Vendor vendor = purchaseOrder.getVendor();
        return new PurchaseOrderDetailResponse(
                purchaseOrder.getPurchaseOrderPublicId(),
                vendor == null ? null : vendor.getVendorPublicId(),
                vendor == null ? null : vendor.getName(),
                purchaseOrder.getOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getTotalAmount(),
                canceledByUserPublicId,
                purchaseOrder.getCanceledAt(),
                items.stream()
                        .map(PurchaseOrderItemResponse::from)
                        .toList());
    }
}
