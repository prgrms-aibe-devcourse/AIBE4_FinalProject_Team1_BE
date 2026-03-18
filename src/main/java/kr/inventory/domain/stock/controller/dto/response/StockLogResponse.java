package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.entity.enums.TransactionType;

public record StockLogResponse(
	OffsetDateTime createdAt,
	String ingredientName,
	UUID batchId,
	TransactionType type,
	BigDecimal changeQuantity,
	IngredientUnit unit,
	BigDecimal balanceAfter,
	String referenceType,
	Long referenceId,
	String workerName
) {
	public static StockLogResponse from(StockLog log) {
		return new StockLogResponse(
			log.getCreatedAt(),
			log.getStockBatch().getProductDisplayName(),
			log.getStockBatch().getBatchPublicId(),
			log.getTransactionType(),
			log.getChangeQuantity(),
			log.getStockBatch().getUnit(),
			log.getBalanceAfter(),
			log.getReferenceType() != null ? log.getReferenceType().name() : null,
			log.getReferenceId(),
			log.getCreatedByUser() != null ? log.getCreatedByUser().getName() : "시스템 자동"
		);
	}
}