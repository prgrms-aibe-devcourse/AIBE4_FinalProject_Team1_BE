package kr.inventory.ai.sales.tool.dto.response;

import java.math.BigDecimal;

public record SalesTrendPointToolResponse(
        String date,
        long orderCount,
        BigDecimal totalAmount
) {
}
