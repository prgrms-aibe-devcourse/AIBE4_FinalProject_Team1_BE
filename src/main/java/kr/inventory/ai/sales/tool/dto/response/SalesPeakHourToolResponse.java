package kr.inventory.ai.sales.tool.dto.response;

public record SalesPeakHourToolResponse(
        int hour,
        String timeRange,
        long orderCount
) {
    public static SalesPeakHourToolResponse from(int hour, long orderCount) {
        return new SalesPeakHourToolResponse(
                hour,
                String.format("%02d:00-%02d:59", hour, hour),
                orderCount
        );
    }
}
