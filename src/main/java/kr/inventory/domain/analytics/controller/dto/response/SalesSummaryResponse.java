package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal averageOrderAmount,
        BigDecimal maxOrderAmount,
        BigDecimal minOrderAmount,
        Double orderCountGrowthRate,
        Double totalAmountGrowthRate,
        Double avgAmountGrowthRate,
        Double maxAmountGrowthRate
) {}
