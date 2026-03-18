package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;

import java.time.LocalDate;
import java.util.Locale;

public record TopMenuRankingToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        Integer topN,
        String rankBy
) {
    public boolean hasExplicitDateRange() {
        return fromDate != null && toDate != null;
    }

    public int resolvedTopN() {
        if (topN == null || topN <= 0) {
            return 10;
        }
        return Math.min(topN, 20);
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
