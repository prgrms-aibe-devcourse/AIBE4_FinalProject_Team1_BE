package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;

import java.time.LocalDate;
import java.util.Locale;

public record SalesTrendToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        String interval,
        String metric
) {
    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public String normalizedInterval() {
        if (interval == null || interval.trim().isEmpty()) {
            return "day";
        }

        return interval.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizedMetric() {
        if (metric == null || metric.trim().isEmpty()) {
            return "both";
        }

        return metric.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
