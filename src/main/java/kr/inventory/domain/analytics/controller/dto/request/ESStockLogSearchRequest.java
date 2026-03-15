package kr.inventory.domain.analytics.controller.dto.request;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;

public record ESStockLogSearchRequest(
	String keyword,
	String transactionType,
	String referenceType,
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	OffsetDateTime startDate,
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	OffsetDateTime endDate,
	int page,
	int size
) {
	public ESStockLogSearchRequest {
		if (size == 0)
			size = 20;
	}
}