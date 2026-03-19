package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.domain.sales.controller.dto.response.SalesOrderItemResponse;

import java.math.BigDecimal;

public record SalesOrderDetailItemToolResponse(
        String menuName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {
    public static SalesOrderDetailItemToolResponse from(SalesOrderItemResponse response) {
        return new SalesOrderDetailItemToolResponse(
                response.menuName(),
                response.price(),
                response.quantity(),
                response.subtotal()
        );
    }
}
