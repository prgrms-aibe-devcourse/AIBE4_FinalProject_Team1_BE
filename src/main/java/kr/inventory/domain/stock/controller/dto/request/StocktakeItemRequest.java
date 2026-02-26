package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record StocktakeItemRequest(
	@NotNull Long ingredientId,
	@NotNull @PositiveOrZero BigDecimal stocktakeQty
) {
}
