package kr.inventory.domain.stock.controller.dto.request;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import kr.inventory.domain.stock.entity.enums.TransactionType;

public record StockLogSearchCondition(
	OffsetDateTime startDate,
	OffsetDateTime endDate,
	TransactionType type,
	String ingredientName
) {
}
