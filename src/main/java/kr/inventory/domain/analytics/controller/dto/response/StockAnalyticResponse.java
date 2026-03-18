package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import kr.inventory.domain.reference.entity.enums.IngredientUnit;

public record StockAnalyticResponse(
	Long ingredientId,
	String ingredientName,
	IngredientUnit unit,

	// 재고 정보
	BigDecimal currentQuantity,

	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	@JsonFormat(pattern = "yyyy-MM-dd")
	LocalDate minExpirationDate,

	boolean isLowStock,
	long activeBatchCount,

	// 폐기 정보
	BigDecimal totalWasteQuantity,
	BigDecimal totalWasteAmount,
	long totalWasteCount

) {
	public static StockAnalyticResponse of(Long id, StockPart stock, WastePart waste) {
		IngredientUnit unit = (stock.unit() != null) ? stock.unit() : waste.unit();

		return new StockAnalyticResponse(
			id,
			stock.name() != null ? stock.name() : waste.name(),
			unit,
			stock.totalQty(),
			stock.minExpiry(),
			stock.isLow(),
			stock.count(),
			waste.totalQty(),
			waste.totalAmount(),
			waste.count()

		);
	}

	public record StockPart(String name, BigDecimal totalQty, LocalDate minExpiry, boolean isLow, long count,
							IngredientUnit unit) {
		public static StockPart empty() {
			return new StockPart(null, BigDecimal.ZERO, null, false, 0L, null);
		}
	}

	public record WastePart(String name, BigDecimal totalQty, BigDecimal totalAmount, long count, IngredientUnit unit) {
		public static WastePart empty() {
			return new WastePart(null, BigDecimal.ZERO, BigDecimal.ZERO, 0L, null);
		}
	}
}