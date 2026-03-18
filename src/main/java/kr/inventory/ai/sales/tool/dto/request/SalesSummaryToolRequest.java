package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;

import java.time.LocalDate;
import java.util.Locale;

public record SalesSummaryToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        String interval,
        String compareMode,
        LocalDate baseFromDate,
        LocalDate baseToDate
) {
    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public String normalizedInterval() {
        if (interval != null && !interval.trim().isEmpty()) {
            return interval.trim().toLowerCase(Locale.ROOT);
        }

        if (period == null) {
            return "day";
        }

        return switch (period) {
            case THIS_WEEK -> "week";
            case THIS_MONTH, LAST_MONTH -> "month";
            default -> "day";
        };
    }

    public String normalizedCompareMode() {
        if ((compareMode == null || compareMode.trim().isEmpty()) && baseFromDate != null && baseToDate != null) {
            return "custom";
        }

        if (compareMode == null || compareMode.trim().isEmpty()) {
            return "previous_period";
        }

        return compareMode.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
