package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.stock.entity.IngredientStockBatch;

public record StockSummaryResponse(
	Long ingredientId,
	String ingredientName,
	BigDecimal totalRemainingQuantity, // 해당 재료의 모든 배치 합계
	IngredientUnit unit,
	Long batchCount,                  // 해당 재료의 유효한 배치 개수
	LocalDate minExpirationDate       // 가장 빠른 유통기한
) {
	// QueryDSL Projections.constructor에서 사용할 생성자 역할을 수행합니다.
	public StockSummaryResponse(
		Long ingredientId,
		String ingredientName,
		BigDecimal totalRemainingQuantity,
		IngredientUnit unit,
		Long batchCount,
		LocalDate minExpirationDate
	) {
		this.ingredientId = ingredientId;
		this.ingredientName = ingredientName;
		this.totalRemainingQuantity = totalRemainingQuantity != null ? totalRemainingQuantity : BigDecimal.ZERO;
		this.unit = unit;
		this.batchCount = batchCount;
		this.minExpirationDate = minExpirationDate;
	}
}
