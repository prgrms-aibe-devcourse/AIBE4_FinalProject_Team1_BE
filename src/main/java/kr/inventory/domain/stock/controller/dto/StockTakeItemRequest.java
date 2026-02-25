package kr.inventory.domain.stock.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record StockTakeItemRequest(
        @NotNull Long ingredientId,
        @NotNull @PositiveOrZero BigDecimal stocktakeQty
) {}
