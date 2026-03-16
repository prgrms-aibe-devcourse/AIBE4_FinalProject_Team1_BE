package kr.inventory.domain.analytics.service.report;

import java.math.BigDecimal;
import java.util.List;

public record SalesSection(
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal averageOrderAmount,
        BigDecimal maxOrderAmount,
        List<MenuEntry> menuTop5
) {
    public record MenuEntry(
            int rank,
            String menuName,
            long totalQuantity,
            BigDecimal totalAmount
    ) {}
}
