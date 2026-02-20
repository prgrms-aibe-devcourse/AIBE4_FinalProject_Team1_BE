package kr.inventory.domain.purchase.controller.dto.response;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineAmount
) {
}
