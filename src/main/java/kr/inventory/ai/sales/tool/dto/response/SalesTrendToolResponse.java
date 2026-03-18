package kr.inventory.ai.sales.tool.dto.response;

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
}
