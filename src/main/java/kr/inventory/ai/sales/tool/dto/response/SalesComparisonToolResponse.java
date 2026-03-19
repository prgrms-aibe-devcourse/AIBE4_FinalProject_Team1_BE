package kr.inventory.ai.sales.tool.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SalesComparisonToolResponse(
        String compareMode,
        List<String> metrics,
        SalesComparisonPeriodToolResponse currentPeriod,
        SalesComparisonPeriodToolResponse basePeriod,
        BigDecimal currentTotalAmount,
        BigDecimal baseTotalAmount,
        BigDecimal amountDelta,
        Double amountDeltaRate,
        long currentOrderCount,
        long baseOrderCount,
        long orderCountDelta,
        Double orderCountDeltaRate,
        BigDecimal currentAverageOrderAmount,
        BigDecimal baseAverageOrderAmount,
        BigDecimal aovDelta,
        Double aovDeltaRate,
        List<SuggestedAction> suggestedActions
) {
}
