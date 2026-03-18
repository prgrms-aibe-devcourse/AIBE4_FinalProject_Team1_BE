package kr.inventory.ai.sales.tool.dto.response;

public record SalesPeakDayToolResponse(
        int dayOfWeek,
        String dayOfWeekLabel,
        long orderCount
) {
}
