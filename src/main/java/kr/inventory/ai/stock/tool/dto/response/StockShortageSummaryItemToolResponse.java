package kr.inventory.ai.stock.tool.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockShortageSummaryItemToolResponse(
	UUID stockShortagePublicId,
	String ingredientName,
	BigDecimal totalShortageAmount,
	Long affectedOrderCount,
    String status,
	OffsetDateTime lastOccurrenceTime
) {
}