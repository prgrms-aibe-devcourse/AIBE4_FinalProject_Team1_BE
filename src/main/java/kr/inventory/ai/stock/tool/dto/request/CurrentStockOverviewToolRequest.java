package kr.inventory.ai.stock.tool.dto.request;

import kr.inventory.ai.stock.tool.enums.StockOverviewSortBy;
import kr.inventory.ai.stock.tool.enums.StockOverviewStatusFilter;

public record CurrentStockOverviewToolRequest(
        String keyword,
        StockOverviewStatusFilter status,
        StockOverviewSortBy sortBy,
        Integer limit
) {
    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    public int resolvedLimit() {
        if (limit == null || limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    public StockOverviewSortBy resolvedSortBy() {
        return sortBy == null ? StockOverviewSortBy.STOCK_ASC : sortBy;
    }
}