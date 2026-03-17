package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;

public record RefundSummaryResponse(
        long refundCount,
        BigDecimal totalRefundAmount,
        double refundRate
) {}
