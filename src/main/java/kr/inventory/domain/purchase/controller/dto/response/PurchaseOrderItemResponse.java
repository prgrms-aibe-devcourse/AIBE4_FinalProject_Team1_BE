package kr.inventory.domain.purchase.controller.dto.response;

import kr.inventory.domain.purchase.entity.PurchaseOrderItem;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
) {
    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getItemName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineAmount()
        );
    }
}
