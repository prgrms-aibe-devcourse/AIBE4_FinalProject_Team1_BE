package kr.inventory.domain.stock.service.command;

import java.math.BigDecimal;

public record IngredientStockTotal(
        Long ingredientId,
        BigDecimal totalQuantity
) {
}
