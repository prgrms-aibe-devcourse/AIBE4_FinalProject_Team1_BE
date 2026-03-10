package kr.inventory.mcp.dto.stock.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GetCurrentStockToolResponse(
        long totalCount,
        List<Item> items
) {
    public record Item(
            UUID ingredientPublicId,
            String ingredientName,
            BigDecimal remainingQuantity,
            String unit
    ) {
    }
}
