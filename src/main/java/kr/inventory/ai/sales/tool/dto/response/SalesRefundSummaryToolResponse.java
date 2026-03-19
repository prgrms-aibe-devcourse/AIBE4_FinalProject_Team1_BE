package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.domain.analytics.controller.dto.response.RefundSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesRefundSummaryToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        long refundCount,
        BigDecimal totalRefundAmount,
        double refundRate,
        BigDecimal affectedNetSales,
        List<SuggestedAction> suggestedFollowUps
) {
    public static SalesRefundSummaryToolResponse from(
            SalesToolDateRange range,
            RefundSummaryResponse response,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new SalesRefundSummaryToolResponse(
                "sales.refund_summary",
                range.preset(),
                range.preset(),
                range.fromDate(),
                range.toDate(),
                response.refundCount(),
                response.totalRefundAmount(),
                response.refundRate(),
                response.totalRefundAmount(),
                suggestedFollowUps
        );
    }
}
