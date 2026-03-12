package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockAnalyticResponse(
	Long ingredientId,
	String ingredientName,

	// 재고 정보
	BigDecimal currentQuantity,
	LocalDate minExpirationDate,
	boolean isLowStock,
	long activeBatchCount,

	// 폐기 정보
	BigDecimal totalWasteQuantity,
	BigDecimal totalWasteAmount,
	long totalWasteCount
) {
	public static StockAnalyticResponse of(Long id, StockPart stock, WastePart waste) {
		return new StockAnalyticResponse(
			id,
			stock.name() != null ? stock.name() : waste.name(),
			stock.totalQty(),
			stock.minExpiry(),
			stock.isLow(),
			stock.count(),
			waste.totalQty(),
			waste.totalAmount(),
			waste.count()
		);
	}

	public record StockPart(String name, BigDecimal totalQty, LocalDate minExpiry, boolean isLow, long count) {
		public static StockPart empty() {
			return new StockPart(null, BigDecimal.ZERO, null, false, 0L);
		}
	}

	public record WastePart(String name, BigDecimal totalQty, BigDecimal totalAmount, long count) {
		public static WastePart empty() {
			return new WastePart(null, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
		}
	}
}