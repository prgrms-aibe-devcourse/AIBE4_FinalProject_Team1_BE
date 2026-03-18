package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GetStockBatchesByIngredientToolResponse(
        UUID ingredientPublicId,
        String ingredientName,
        String unit,
        BigDecimal totalRemainingQuantity,
        BigDecimal lowStockThreshold,
        int count,
        List<StockBatchItemToolResponse> batches
) {

    public static GetStockBatchesByIngredientToolResponse of(
            Ingredient ingredient,
            List<IngredientStockBatch> stockBatches
    ) {
        LocalDate today = LocalDate.now();

        List<StockBatchItemToolResponse> items = stockBatches.stream()
                .map(batch -> StockBatchItemToolResponse.from(
                        batch,
                        resolveInboundDate(batch),
                        today
                ))
                .toList();

        BigDecimal totalRemainingQuantity = calculateTotalRemaining(stockBatches);

        return new GetStockBatchesByIngredientToolResponse(
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                ingredient.getUnit().name(),
                totalRemainingQuantity,
                ingredient.getLowStockThreshold(),
                items.size(),
                items
        );
    }

    private static BigDecimal calculateTotalRemaining(List<IngredientStockBatch> batches) {
        return batches.stream()
                .map(IngredientStockBatch::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static LocalDate resolveInboundDate(IngredientStockBatch batch) {
        if (batch.getInboundItem() != null
                && batch.getInboundItem().getInbound() != null
                && batch.getInboundItem().getInbound().getCreatedAt() != null) {
            return batch.getInboundItem().getInbound().getCreatedAt().toLocalDate();
        }

        return batch.getCreatedAt() != null
                ? batch.getCreatedAt().toLocalDate()
                : null;
    }
}