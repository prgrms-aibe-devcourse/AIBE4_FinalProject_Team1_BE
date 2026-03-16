package kr.inventory.domain.analytics.service.report;

import java.math.BigDecimal;

public record RefundSection(
        long refundCount,
        BigDecimal totalRefundAmount,
        double refundRate          // 환불건수 / (완료건수 + 환불건수) * 100
) {}
