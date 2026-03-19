package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;

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
    public static SalesSummaryToolResponse from(
            SalesToolDateRange currentRange,
            SalesToolDateRange baseRange,
            String interval,
            String compareMode,
            SalesSummaryResponse current,
            SalesSummaryResponse base,
            Double orderCountGrowthRate,
            Double totalAmountGrowthRate,
            Double avgAmountGrowthRate,
            Double maxAmountGrowthRate,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new SalesSummaryToolResponse(
                "sales.summary",
                currentRange.preset(),
                currentRange.preset(),
                currentRange.fromDate(),
                currentRange.toDate(),
                interval,
                compareMode,
                baseRange.fromDate(),
                baseRange.toDate(),
                current.totalOrderCount(),
                current.totalAmount(),
                current.averageOrderAmount(),
                current.maxOrderAmount(),
                current.minOrderAmount(),
                orderCountGrowthRate,
                totalAmountGrowthRate,
                avgAmountGrowthRate,
                maxAmountGrowthRate,
                suggestedFollowUps
        );
    }
}
