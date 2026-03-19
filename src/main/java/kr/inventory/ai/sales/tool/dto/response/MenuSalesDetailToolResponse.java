package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.domain.analytics.controller.dto.response.MenuSalesDetailResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public record MenuSalesDetailToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String menuName,
        long totalQuantity,
        BigDecimal totalAmount,
        BigDecimal averageSellingPrice,
        BigDecimal salesShareRate,
        List<SuggestedAction> suggestedFollowUps
) {
    public static MenuSalesDetailToolResponse from(
            SalesToolDateRange range,
            MenuSalesDetailResponse response,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new MenuSalesDetailToolResponse(
                "sales.menu_sales_detail",
                range.preset(),
                range.preset(),
                range.fromDate(),
                range.toDate(),
                response.menuName(),
                response.totalQuantity(),
                response.totalAmount(),
                response.averageSellingPrice(),
                BigDecimal.valueOf(response.salesShareRate()).setScale(2, RoundingMode.HALF_UP),
                suggestedFollowUps
        );
    }
}
