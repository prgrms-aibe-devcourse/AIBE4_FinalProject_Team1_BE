package kr.inventory.ai.stock.tool.dto.request;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record StockShortageSummaryToolRequest(
        String keyword,
        OffsetDateTime from,
        OffsetDateTime to
) {

    private static final int DEFAULT_LIMIT = 5;
    private static final int DEFAULT_DAYS = 7;

    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    public OffsetDateTime resolvedFrom() {
        if (from != null) {
            return from;
        }
        return OffsetDateTime.now(ZoneId.systemDefault()).minusDays(DEFAULT_DAYS);
    }

    public OffsetDateTime resolvedTo() {
        if (to != null) {
            return to;
        }
        return OffsetDateTime.now(ZoneId.systemDefault());
    }
}