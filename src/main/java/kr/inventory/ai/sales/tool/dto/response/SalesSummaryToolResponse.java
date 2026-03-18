package kr.inventory.ai.sales.tool.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesSummaryToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String interval,
        String compareMode,
        LocalDate baseFromDate,
        LocalDate baseToDate,
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal averageOrderAmount,
        BigDecimal maxOrderAmount,
        BigDecimal minOrderAmount,
        Double orderCountGrowthRate,
        Double totalAmountGrowthRate,
        Double avgAmountGrowthRate,
        Double maxAmountGrowthRate,
        List<SuggestedAction> suggestedFollowUps
) {
}
