package kr.inventory.domain.stock.controller.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockInboundItemRequest(@NotNull Long ingredientId,
									  @NotNull String rawProductName,
									  @NotNull @Positive BigDecimal quantity,
									  @Positive BigDecimal unitCost,
									  LocalDate expirationDate) {
}
