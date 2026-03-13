package kr.inventory.domain.stock.repository.dto;

import java.math.BigDecimal;

public record IngredientStockTotalDto(
        Long ingredientId,
        BigDecimal totalQuantity
) {
}
