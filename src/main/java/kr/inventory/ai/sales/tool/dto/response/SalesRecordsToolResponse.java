package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.tool.support.SalesToolDateRange;

import java.time.LocalDate;
import java.util.List;

public record SalesRecordsToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String statusFilter,
        String typeFilter,
        SalesRecordsSummaryToolResponse summary,
        int count,
        List<SalesRecordItemToolResponse> orders,
        SalesRecordsPageInfoToolResponse pageInfo,
        List<SuggestedAction> suggestedFollowUps
) {
    public static SalesRecordsToolResponse from(
            SalesToolDateRange range,
            String statusFilter,
            String typeFilter,
            SalesRecordsSummaryToolResponse summary,
            List<SalesRecordItemToolResponse> orders,
            SalesRecordsPageInfoToolResponse pageInfo,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new SalesRecordsToolResponse(
                "sales.records",
                range.preset(),
                range.preset(),
                range.fromDate(),
                range.toDate(),
                statusFilter,
                typeFilter,
                summary,
                orders.size(),
                orders,
                pageInfo,
                suggestedFollowUps
        );
    }
}
