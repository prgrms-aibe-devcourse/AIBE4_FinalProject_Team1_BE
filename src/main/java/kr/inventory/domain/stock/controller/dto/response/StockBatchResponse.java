package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;

public record StockBatchResponse(
	UUID stockBatchId,
	String rawProductName,
	BigDecimal remainingQuantity,
	LocalDate expirationDate,
	OffsetDateTime createdAt,
	StockBatchStatus status
) {
	public static StockBatchResponse from(IngredientStockBatch batch) {
		return new StockBatchResponse(
			batch.getBatchPublicId(),
			batch.getProductDisplayName(),
			batch.getRemainingQuantity(),
			batch.getExpirationDate(),
			batch.getCreatedAt(),
			batch.getStatus()
		);
	}
}