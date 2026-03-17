package kr.inventory.ai.stock.tool.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record StockShortageSummaryItemToolResponse(
        Long ingredientId,
        String ingredientName,
        BigDecimal totalShortageAmount,
        Long affectedOrderCount,
        OffsetDateTime lastOccurrenceTime,
        List<Long> relatedOrderIds
) {
}