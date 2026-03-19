package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.constant.SalesConstants;

import java.time.DayOfWeek;
import java.time.format.TextStyle;

public record SalesPeakDayToolResponse(
        int dayOfWeek,
        String dayOfWeekLabel,
        long orderCount
) {
    public static SalesPeakDayToolResponse from(int dayOfWeek, long orderCount) {
        return new SalesPeakDayToolResponse(
                dayOfWeek,
                DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, SalesConstants.DAY_LABEL_LOCALE),
                orderCount
        );
    }
}
