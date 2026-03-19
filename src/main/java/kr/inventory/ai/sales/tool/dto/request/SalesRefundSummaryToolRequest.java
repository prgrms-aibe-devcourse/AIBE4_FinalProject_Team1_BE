package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;

import java.time.LocalDate;

public record SalesRefundSummaryToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate
) {
}
