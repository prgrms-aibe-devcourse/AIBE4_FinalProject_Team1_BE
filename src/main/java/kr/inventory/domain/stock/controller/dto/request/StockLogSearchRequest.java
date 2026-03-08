package kr.inventory.domain.stock.controller.dto.request;

import java.time.OffsetDateTime;

import kr.inventory.domain.stock.entity.enums.TransactionType;

public record StockLogSearchRequest(
	OffsetDateTime startDate,
	OffsetDateTime endDate,
	TransactionType type,
	String ingredientName
) {
}
