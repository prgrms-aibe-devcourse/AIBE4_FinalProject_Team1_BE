package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderSummaryResponse(
        UUID purchaseOrderPublicId,
        Long storeId,
        UUID vendorPublicId,
        String vendorName,
        String orderNo,
        PurchaseOrderStatus status,
        BigDecimal totalAmount) {
    public static PurchaseOrderSummaryResponse from(PurchaseOrder purchaseOrder) {
        Vendor vendor = purchaseOrder.getVendor();
        return new PurchaseOrderSummaryResponse(
                purchaseOrder.getPurchaseOrderPublicId(),
                purchaseOrder.getStore().getStoreId(),
                vendor == null ? null : vendor.getVendorPublicId(),
                vendor == null ? null : vendor.getName(),
                purchaseOrder.getOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getTotalAmount());
    }
}
