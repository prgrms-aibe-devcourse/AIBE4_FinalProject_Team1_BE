package kr.inventory.domain.reference.controller.dto;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientResponse(
        UUID ingredientPublicId,
        String name,
        IngredientUnit unit,
        BigDecimal lowStockThreshold,
        IngredientStatus status
) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                ingredient.getUnit(),
                ingredient.getLowStockThreshold(),
                ingredient.getStatus()
        );
    }
}
