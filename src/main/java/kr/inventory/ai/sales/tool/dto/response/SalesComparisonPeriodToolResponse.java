package kr.inventory.ai.sales.tool.dto.response;

import java.time.LocalDate;

public record SalesComparisonPeriodToolResponse(
        String preset,
        LocalDate fromDate,
        LocalDate toDate
) {
}
