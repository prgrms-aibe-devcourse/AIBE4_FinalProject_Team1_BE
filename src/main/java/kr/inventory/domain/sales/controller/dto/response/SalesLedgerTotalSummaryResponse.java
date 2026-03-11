package kr.inventory.domain.sales.controller.dto.response;

import java.math.BigDecimal;

public record SalesLedgerTotalSummaryResponse(
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal totalRefundAmount,
        BigDecimal totalNetAmount
) {
}
