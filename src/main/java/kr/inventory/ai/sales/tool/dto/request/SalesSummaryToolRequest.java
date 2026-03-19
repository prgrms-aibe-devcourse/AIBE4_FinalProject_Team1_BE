package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.exception.SalesErrorCode;
import kr.inventory.ai.sales.exception.SalesException;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public record SalesSummaryToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        String interval,
        String compareMode,
        LocalDate baseFromDate,
        LocalDate baseToDate
) {
    private static final Set<String> SUPPORTED_INTERVALS = Set.of("day", "week", "month");
    private static final Set<String> SUPPORTED_COMPARE_MODES = Set.of(
            "previous_period",
            "same_period_last_week",
            "same_period_last_month",
            "custom"
    );

    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public SalesToolDateRange resolvedDateRange() {
        return SalesToolDateRangeResolver.resolve(period, fromDate, toDate, DateRangePreset.LAST_7_DAYS);
    }

    public String resolvedInterval() {
        String normalized = normalizedInterval();
        if (!SUPPORTED_INTERVALS.contains(normalized)) {
            throw new IllegalArgumentException("interval must be day, week, or month");
        }
        return normalized;
    }

    public String resolvedCompareMode() {
        String normalized = normalizedCompareMode();
        if (!SUPPORTED_COMPARE_MODES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "compareMode must be previous_period, same_period_last_week, same_period_last_month, or custom"
            );
        }
        return normalized;
    }

    public LocalDate resolvedBaseFromDate() {
        if (!"custom".equals(resolvedCompareMode())) {
            return baseFromDate;
        }

        if (baseFromDate == null || baseToDate == null) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }
        return baseFromDate;
    }

    public LocalDate resolvedBaseToDate() {
        if (!"custom".equals(resolvedCompareMode())) {
            return baseToDate;
        }

        if (baseFromDate == null || baseToDate == null) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }
        return baseToDate;
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
