package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public record SalesTrendToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        String interval,
        String metric
) {
    private static final Set<String> SUPPORTED_INTERVALS = Set.of("day", "week", "month");
    private static final Set<String> SUPPORTED_METRICS = Set.of("amount", "order_count", "both");

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

    public String resolvedMetric() {
        String normalized = normalizedMetric();
        if (!SUPPORTED_METRICS.contains(normalized)) {
            throw new IllegalArgumentException("metric must be amount, order_count, or both");
        }
        return normalized;
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
