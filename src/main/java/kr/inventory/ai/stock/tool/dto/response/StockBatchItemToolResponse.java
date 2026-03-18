package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.entity.IngredientStockBatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record StockBatchItemToolResponse(
        UUID batchPublicId,
        BigDecimal remainingQuantity,
        LocalDate inboundDate,
        LocalDate expirationDate,
        Long daysToExpire,
        String batchStatus,
        String sourceType,
        String productDisplayName
) {
    public static StockBatchItemToolResponse from(
            IngredientStockBatch batch,
            LocalDate inboundDate,
            LocalDate today
    ) {
        LocalDate expirationDate = batch.getExpirationDate();

        Long daysToExpire = expirationDate == null
                ? null
                : ChronoUnit.DAYS.between(today, expirationDate);

        return new StockBatchItemToolResponse(
                batch.getBatchPublicId(),
                batch.getRemainingQuantity(),
                inboundDate,
                expirationDate,
                daysToExpire,
                batch.getStatus().name(),
                batch.getSourceType() != null ? batch.getSourceType().name() : null,
                batch.getProductDisplayName()
        );
    }
}