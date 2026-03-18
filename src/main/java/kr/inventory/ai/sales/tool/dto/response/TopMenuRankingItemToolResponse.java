package kr.inventory.ai.sales.tool.dto.response;

import java.math.BigDecimal;

public record TopMenuRankingItemToolResponse(
        int rank,
        String menuName,
        long totalQuantity,
        BigDecimal totalAmount,
        BigDecimal amountShareRate
) {
}
