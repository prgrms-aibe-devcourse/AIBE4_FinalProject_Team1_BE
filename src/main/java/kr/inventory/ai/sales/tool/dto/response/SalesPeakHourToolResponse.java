package kr.inventory.ai.sales.tool.dto.response;

public record SalesPeakHourToolResponse(
        int hour,
        String timeRange,
        long orderCount
) {
}
