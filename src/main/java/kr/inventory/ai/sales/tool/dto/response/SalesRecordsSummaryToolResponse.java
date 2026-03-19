package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.domain.sales.controller.dto.response.SalesLedgerTotalSummaryResponse;

import java.math.BigDecimal;

public record SalesRecordsSummaryToolResponse(
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal totalRefundAmount,
        BigDecimal totalNetAmount
) {
    public static SalesRecordsSummaryToolResponse from(SalesLedgerTotalSummaryResponse response) {
        return new SalesRecordsSummaryToolResponse(
                response.totalOrderCount(),
                response.totalAmount(),
                response.totalRefundAmount(),
                response.totalNetAmount()
        );
    }
}
