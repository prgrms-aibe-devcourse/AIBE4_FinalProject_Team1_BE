package kr.inventory.domain.catalog.controller.dto;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.entity.enums.IngredientStatus;
import kr.inventory.domain.catalog.entity.enums.IngredientUnit;

import java.math.BigDecimal;

public record IngredientResponse(
        Long ingredientId,
        String name,
        IngredientUnit unit,
        BigDecimal lowStockThreshold,
        IngredientStatus status
) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getIngredientId(),
                ingredient.getName(),
                ingredient.getUnit(),
                ingredient.getLowStockThreshold(),
                ingredient.getStatus()
        );
    }
}
