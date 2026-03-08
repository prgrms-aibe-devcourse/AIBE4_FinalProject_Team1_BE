package kr.inventory.domain.stock.controller.dto.request;

import java.time.OffsetDateTime;

import kr.inventory.domain.stock.entity.enums.WasteReason;

public record WasteSearchRequest(
	OffsetDateTime startAt,
	OffsetDateTime endAt,
	WasteReason reason,
	String ingredientName
) {
}
