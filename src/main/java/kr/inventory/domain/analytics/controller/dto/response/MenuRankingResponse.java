package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;

public record MenuRankingResponse(
        int rank,
        String menuName,
        long totalQuantity,
        BigDecimal totalAmount
) {}
