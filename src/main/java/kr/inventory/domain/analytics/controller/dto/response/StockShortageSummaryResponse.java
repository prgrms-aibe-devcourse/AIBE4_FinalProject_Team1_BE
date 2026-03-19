package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockShortageSummaryResponse(
	UUID stockShortagePublicId,
	String ingredientName,
	BigDecimal totalShortageAmount,
	String status,
	Long affectedOrderCount,
	OffsetDateTime lastOccurrenceTime) {

	public static StockShortageSummaryResponse of(UUID stockShortagePublicId, String ingredientName,
		BigDecimal totalShortageAmount, String status, Long affectedOrderCount, OffsetDateTime lastOccurrenceTime) {
		return new StockShortageSummaryResponse(stockShortagePublicId, ingredientName, totalShortageAmount, status,
			affectedOrderCount,
			lastOccurrenceTime);
	}
}
