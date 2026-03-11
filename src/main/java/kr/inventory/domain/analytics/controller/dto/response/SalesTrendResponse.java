package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;

public record SalesTrendResponse(
        String date,          // "2024-03-01" 형태
        long orderCount,
        BigDecimal totalAmount
) {}
