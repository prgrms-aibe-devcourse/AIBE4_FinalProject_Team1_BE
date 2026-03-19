package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public record TopMenuRankingToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        Integer topN,
        String rankBy
) {
    private static final Set<String> SUPPORTED_RANK_BY = Set.of("quantity", "amount");

    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public SalesToolDateRange resolvedDateRange() {
        return SalesToolDateRangeResolver.resolve(period, fromDate, toDate, DateRangePreset.LAST_7_DAYS);
    }

    public int resolvedTopN() {
        if (topN == null || topN <= 0) {
            return 10;
        }
        return Math.min(topN, 20);
    }

    public String resolvedRankBy() {
        String normalized = normalizedRankBy();
        if (!SUPPORTED_RANK_BY.contains(normalized)) {
            throw new IllegalArgumentException("rankBy must be quantity or amount");
        }
        return normalized;
    }

    public String normalizedRankBy() {
        if (rankBy == null || rankBy.trim().isEmpty()) {
            return "quantity";
        }

        return rankBy.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
