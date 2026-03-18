package kr.inventory.domain.analytics.controller.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public record ESStockLogSearchRequest(
	String keyword,
	String transactionType,
	String referenceType,
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	OffsetDateTime startDate,
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	OffsetDateTime endDate,
    Integer limit
) {
    public int resolvedLimit() {
        if (limit == null || limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}