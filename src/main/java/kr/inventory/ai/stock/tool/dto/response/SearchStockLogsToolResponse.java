package kr.inventory.ai.stock.tool.dto.response;

import java.util.List;

public record SearchStockLogsToolResponse(
        int count,
        List<StockLogItemToolResponse> logs
) {
}
