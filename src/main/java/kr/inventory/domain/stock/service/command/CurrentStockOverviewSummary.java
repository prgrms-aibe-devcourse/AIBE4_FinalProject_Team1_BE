package kr.inventory.domain.stock.service.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrentStockOverviewSummary(
        Long ingredientId,
        UUID ingredientPublicId,
        String ingredientName,
        String normalizedIngredientName,
        String ingredientStatus,
        String unit,
        BigDecimal lowStockThreshold,
        BigDecimal currentQuantity,
        String stockStatus,
        boolean belowThreshold
) {
}
