package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.service.command.CurrentStockOverviewSummary;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrentStockOverviewItemToolResponse(
        UUID ingredientPublicId,
        String ingredientName,
        String unit,
        BigDecimal currentQuantity,
        BigDecimal lowStockThreshold,
        String stockStatus,
        boolean belowThreshold
) {
    public static CurrentStockOverviewItemToolResponse from(CurrentStockOverviewSummary summary) {
        return new CurrentStockOverviewItemToolResponse(
                summary.ingredientPublicId(),
                summary.ingredientName(),
                summary.unit(),
                summary.currentQuantity(),
                summary.lowStockThreshold(),
                summary.stockStatus(),
                summary.belowThreshold()
        );
    }
}
