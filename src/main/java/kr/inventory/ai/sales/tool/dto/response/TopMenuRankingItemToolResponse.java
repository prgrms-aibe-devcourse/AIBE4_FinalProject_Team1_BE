package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record TopMenuRankingItemToolResponse(
        int rank,
        String menuName,
        long totalQuantity,
        BigDecimal totalAmount,
        BigDecimal amountShareRate
) {
    public static TopMenuRankingItemToolResponse from(MenuRankingResponse response, BigDecimal totalSalesAmount) {
        return new TopMenuRankingItemToolResponse(
                response.rank(),
                response.menuName(),
                response.totalQuantity(),
                response.totalAmount(),
                calculateAmountShareRate(response.totalAmount(), totalSalesAmount)
        );
    }

    private static BigDecimal calculateAmountShareRate(BigDecimal menuAmount, BigDecimal totalSalesAmount) {
        if (menuAmount == null || totalSalesAmount == null || totalSalesAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return menuAmount
                .divide(totalSalesAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
