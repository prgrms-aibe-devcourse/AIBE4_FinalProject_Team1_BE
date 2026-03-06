package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record StockTakeItemRequest(
    @NotNull UUID ingredientPublicId,
	@NotNull @PositiveOrZero BigDecimal stockTakeQty
) {
}
