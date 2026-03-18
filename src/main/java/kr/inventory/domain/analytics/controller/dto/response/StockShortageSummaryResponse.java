package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record StockShortageSummaryResponse(
	Long ingredientId,
	String ingredientName,
	BigDecimal totalShortageAmount,
	String status,
	Long affectedOrderCount,
	OffsetDateTime lastOccurrenceTime,
	List<Long> relatedOrderIds) {

	public static StockShortageSummaryResponse of(Long ingredientId, String ingredientName,
		BigDecimal totalShortageAmount, String status, Long affectedOrderCount, OffsetDateTime lastOccurrenceTime,
		List<Long> relatedOrderIds) {
		return new StockShortageSummaryResponse(ingredientId, ingredientName, totalShortageAmount, status,
			affectedOrderCount,
			lastOccurrenceTime, relatedOrderIds);
	}
}
