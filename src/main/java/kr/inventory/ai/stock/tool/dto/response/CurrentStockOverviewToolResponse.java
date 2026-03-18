package kr.inventory.ai.stock.tool.dto.response;

import java.util.List;

public record CurrentStockOverviewToolResponse(
        int count,
        List<CurrentStockOverviewItemToolResponse> items
) {
    public static CurrentStockOverviewToolResponse of(List<CurrentStockOverviewItemToolResponse> items) {
        return new CurrentStockOverviewToolResponse(items.size(), items);
    }
}
