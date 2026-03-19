package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;

import java.time.LocalDate;

public record MenuSalesDetailToolRequest(
        String menuName,
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate
) {
    public String normalizedMenuName() {
        if (menuName == null) {
            return null;
        }

        String trimmed = menuName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
