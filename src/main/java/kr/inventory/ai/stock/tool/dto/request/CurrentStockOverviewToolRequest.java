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

    public Integer resolvedLimit() {
        if (limit == null || limit <= 0) {
            return null;
        }
        return Math.min(limit, 100);
    }

    public StockOverviewSortBy resolvedSortBy() {
        return sortBy == null ? StockOverviewSortBy.NAME_ASC : sortBy;
    }
}