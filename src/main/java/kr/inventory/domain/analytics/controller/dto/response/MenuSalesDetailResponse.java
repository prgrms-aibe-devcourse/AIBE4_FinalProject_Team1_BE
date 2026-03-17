package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;

public record MenuSalesDetailResponse(
        String menuName,
        long totalQuantity,
        BigDecimal totalAmount,
        BigDecimal averageSellingPrice,  // totalAmount / totalQuantity
        double salesShareRate        // 전체 매출 대비 해당 메뉴 매출 비율 (%)
) {}
