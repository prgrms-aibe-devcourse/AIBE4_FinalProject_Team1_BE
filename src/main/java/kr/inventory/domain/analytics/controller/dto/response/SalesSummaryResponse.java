package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal averageOrderAmount,  // 객단가
        BigDecimal maxOrderAmount,
        BigDecimal minOrderAmount
) {}
