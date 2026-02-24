package kr.inventory.domain.stock.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockInboundItemRequest(@NotNull Long ingredientId,
									  @NotNull @Positive BigDecimal quantity,
									  @Positive BigDecimal unitCost,
									  LocalDate expirationDate) {
}
