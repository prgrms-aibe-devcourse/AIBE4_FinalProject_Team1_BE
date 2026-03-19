package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public record SalesPeakToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        Integer limit,
        String viewType
) {
    private static final Set<String> SUPPORTED_VIEW_TYPES = Set.of("combined", "day_only", "hour_only");

    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public SalesToolDateRange resolvedDateRange() {
        return SalesToolDateRangeResolver.resolve(period, fromDate, toDate, DateRangePreset.LAST_7_DAYS);
    }

    public int resolvedLimit() {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    public String resolvedViewType() {
        String normalized = normalizedViewType();
        if (!SUPPORTED_VIEW_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("viewType must be combined, day_only, or hour_only");
        }
        return normalized;
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
