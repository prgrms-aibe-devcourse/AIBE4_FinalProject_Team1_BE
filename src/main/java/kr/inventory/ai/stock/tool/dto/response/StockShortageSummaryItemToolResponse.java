package kr.inventory.ai.stock.tool.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockShortageSummaryItemToolResponse(
	UUID ingredientId,
	String ingredientName,
	BigDecimal totalShortageAmount,
	Long affectedOrderCount,
	OffsetDateTime lastOccurrenceTime
) {
}