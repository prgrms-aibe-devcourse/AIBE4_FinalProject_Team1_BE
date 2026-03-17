package kr.inventory.ai.stock.tool.dto.response;

import java.util.List;

public record StockShortageSummaryToolResponse(
        int count,
        List<StockShortageSummaryItemToolResponse> shortages
) {
}
