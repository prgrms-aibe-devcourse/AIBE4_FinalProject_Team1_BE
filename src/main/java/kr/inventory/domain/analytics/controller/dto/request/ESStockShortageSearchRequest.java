package kr.inventory.domain.analytics.controller.dto.request;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;

public record ESStockShortageSearchRequest(
	String keyword,

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	OffsetDateTime from,

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	OffsetDateTime to

) {
}
