package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.tool.support.SalesToolDateRange;

import java.time.LocalDate;
import java.util.List;

public record SalesTrendToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String interval,
        String metric,
        int count,
        SalesTrendPointToolResponse highestPoint,
        SalesTrendPointToolResponse lowestPoint,
        SalesTrendPointToolResponse latestPoint,
        Double overallChangeRate,
        List<SalesTrendPointToolResponse> trend,
        List<SuggestedAction> suggestedFollowUps
) {
    public static SalesTrendToolResponse from(
            SalesToolDateRange range,
            String interval,
            String metric,
            List<SalesTrendPointToolResponse> trend,
            SalesTrendPointToolResponse highestPoint,
            SalesTrendPointToolResponse lowestPoint,
            SalesTrendPointToolResponse latestPoint,
            Double overallChangeRate,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new SalesTrendToolResponse(
                "sales.trend",
                range.preset(),
                range.preset(),
                range.fromDate(),
                range.toDate(),
                interval,
                metric,
                trend.size(),
                highestPoint,
                lowestPoint,
                latestPoint,
                overallChangeRate,
                trend,
                suggestedFollowUps
        );
    }
}
