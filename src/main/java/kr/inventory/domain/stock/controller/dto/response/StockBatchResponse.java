package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;

public record StockBatchResponse(
	Long stockBatchId,
	BigDecimal remainingQuantity,
	LocalDate expirationDate,
	OffsetDateTime createdAt,
	StockBatchStatus status
) {
	public static StockBatchResponse from(IngredientStockBatch batch) {
		return new StockBatchResponse(
			batch.getBatchId(),
			batch.getRemainingQuantity(),
			batch.getExpirationDate(),
			batch.getCreatedAt(),
			batch.getStatus()
		);
	}
}