package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.domain.analytics.controller.dto.response.SalesTrendResponse;

import java.math.BigDecimal;

public record SalesTrendPointToolResponse(
        String date,
        long orderCount,
        BigDecimal totalAmount
) {
    public static SalesTrendPointToolResponse from(SalesTrendResponse response) {
        return new SalesTrendPointToolResponse(
                response.date(),
                response.orderCount(),
                response.totalAmount()
        );
    }
}
