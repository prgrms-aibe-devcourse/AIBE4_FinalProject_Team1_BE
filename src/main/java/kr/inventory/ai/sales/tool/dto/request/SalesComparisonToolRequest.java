package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record SalesComparisonToolRequest(
        DateRangePreset currentPeriod,
        LocalDate currentFromDate,
        LocalDate currentToDate,
        String compareMode,
        LocalDate baseFromDate,
        LocalDate baseToDate,
        List<String> metrics
) {
    public SalesToolDateRange resolvedCurrentDateRange() {
        return SalesToolDateRangeResolver.resolve(currentPeriod, currentFromDate, currentToDate, DateRangePreset.LAST_7_DAYS);
    }

    public String resolvedCompareMode() {
        if (compareMode == null || compareMode.trim().isEmpty()) {
            return "previous_period";
        }

        String normalized = compareMode.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "previous_period", "same_period_last_week", "same_period_last_month", "custom" -> normalized;
            default -> "previous_period";
        };
    }

    public LocalDate resolvedBaseFromDate() {
        return baseFromDate;
    }

    public LocalDate resolvedBaseToDate() {
        return baseToDate;
    }

    public List<String> resolvedMetrics() {
        if (metrics == null || metrics.isEmpty()) {
            return List.of("amount", "order_count", "aov");
        }

        Set<String> resolved = new LinkedHashSet<>();
        for (String metric : metrics) {
            if (metric == null || metric.trim().isEmpty()) {
                continue;
            }

            String normalized = metric.trim()
                    .toLowerCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');

            switch (normalized) {
                case "amount", "sales", "total_amount" -> resolved.add("amount");
                case "order_count", "orders", "ordercount" -> resolved.add("order_count");
                case "aov", "average_order_value", "average_order_amount", "avg_order_amount" -> resolved.add("aov");
                default -> {
                    // 알 수 없는 metric은 무시
                }
            }
        }

        if (resolved.isEmpty()) {
            return List.of("amount", "order_count", "aov");
        }

        return List.copyOf(resolved);
    }
}
