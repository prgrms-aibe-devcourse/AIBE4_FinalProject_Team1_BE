package kr.inventory.domain.analytics.service.report;

import java.math.BigDecimal;
import java.util.List;

public record WasteSection(
        BigDecimal totalWasteAmount,
        long totalWasteCount,
        List<ReasonEntry> reasonBreakdown,
        List<IngredientEntry> top5Ingredients
) {
    public record ReasonEntry(
            String reason,
            long count,
            BigDecimal wasteAmount,
            double ratio
    ) {}

    public record IngredientEntry(
            String ingredientName,
            BigDecimal wasteQuantity,
            String unit,
            BigDecimal wasteAmount
    ) {}
}
