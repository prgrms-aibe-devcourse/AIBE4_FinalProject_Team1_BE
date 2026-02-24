package kr.inventory.domain.stock.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import kr.inventory.domain.stock.entity.StockInboundItem;

public record StockInboundItemResponse(
	Long inboundItemId,
	Long inboundId,         // 어떤 입고에 속하는지 ID만 포함
	Long ingredientId,
	String ingredientName,  // 화면 표시용 식재료 이름
	BigDecimal quantity,
	BigDecimal unitCost,
	LocalDate expirationDate
) {
	public static StockInboundItemResponse from(StockInboundItem item) {
		return new StockInboundItemResponse(
			item.getInboundItemId(),
			item.getInbound().getInboundId(),
			item.getIngredient().getIngredientId(),
			item.getIngredient().getName(),
			item.getQuantity(),
			item.getUnitCost(),
			item.getExpirationDate()
		);
	}
}
