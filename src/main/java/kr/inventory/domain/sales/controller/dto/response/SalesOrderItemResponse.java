package kr.inventory.domain.sales.controller.dto.response;

import kr.inventory.domain.sales.entity.SalesOrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderItemResponse(
        UUID menuPublicId,
        String menuName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {
    public static SalesOrderItemResponse from(SalesOrderItem item) {
        return new SalesOrderItemResponse(
                item.getMenu().getMenuPublicId(),
                item.getMenuName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
