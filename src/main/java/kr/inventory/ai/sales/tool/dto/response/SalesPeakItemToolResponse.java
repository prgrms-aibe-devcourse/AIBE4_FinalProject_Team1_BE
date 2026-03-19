package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.domain.analytics.controller.dto.response.SalesPeakResponse;

import java.time.DayOfWeek;
import java.time.format.TextStyle;

public record SalesPeakItemToolResponse(
        int dayOfWeek,
        String dayOfWeekLabel,
        int hour,
        String timeRange,
        long orderCount
) {
    public static SalesPeakItemToolResponse from(SalesPeakResponse response) {
        return new SalesPeakItemToolResponse(
                response.dayOfWeek(),
                DayOfWeek.of(response.dayOfWeek()).getDisplayName(TextStyle.FULL, SalesConstants.DAY_LABEL_LOCALE),
                response.hour(),
                String.format("%02d:00-%02d:59", response.hour(), response.hour()),
                response.orderCount()
        );
    }
}
