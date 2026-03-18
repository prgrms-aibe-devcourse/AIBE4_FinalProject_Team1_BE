package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;

import java.time.LocalDate;
import java.util.Locale;

public record SalesPeakToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        Integer limit,
        String viewType
) {
    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public int resolvedLimit() {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    public String normalizedViewType() {
        if (viewType == null || viewType.trim().isEmpty()) {
            return "combined";
        }

        return viewType.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
