package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.ai.sales.tool.support.SalesToolDateRange;

import java.time.LocalDate;
import java.util.List;

public record SalesPeakToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String viewType,
        int topTimeSlotCount,
        List<SalesPeakItemToolResponse> topTimeSlots,
        int topDayCount,
        List<SalesPeakDayToolResponse> topDays,
        int topHourCount,
        List<SalesPeakHourToolResponse> topHours,
        SalesPeakDayToolResponse bestDayOfWeek,
        SalesPeakHourToolResponse bestHour,
        List<SuggestedAction> suggestedFollowUps
) {
    public static SalesPeakToolResponse from(
            SalesToolDateRange range,
            String viewType,
            List<SalesPeakItemToolResponse> topTimeSlots,
            List<SalesPeakDayToolResponse> topDays,
            List<SalesPeakHourToolResponse> topHours,
            SalesPeakDayToolResponse bestDayOfWeek,
            SalesPeakHourToolResponse bestHour,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new SalesPeakToolResponse(
                "sales.peak",
                range.preset(),
                range.preset(),
                range.fromDate(),
                range.toDate(),
                viewType,
                topTimeSlots.size(),
                topTimeSlots,
                topDays.size(),
                topDays,
                topHours.size(),
                topHours,
                bestDayOfWeek,
                bestHour,
                suggestedFollowUps
        );
    }
}
