package kr.inventory.ai.sales.tool.dto.response;

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
}
