package kr.inventory.ai.sales.tool.dto.response;

public record SalesPeakItemToolResponse(
        int dayOfWeek,
        String dayOfWeekLabel,
        int hour,
        String timeRange,
        long orderCount
) {
}
