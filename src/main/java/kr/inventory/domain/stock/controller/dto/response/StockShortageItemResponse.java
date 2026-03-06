package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.StockShortage;

import java.math.BigDecimal;
import java.util.UUID;

public record StockShortageItemResponse(
        UUID stockShortagePublicId,
        UUID ingredientPublicId,
        String ingredientName,
        BigDecimal requiredAmount,
        BigDecimal shortageAmount
) {
    public static StockShortageItemResponse from(StockShortage shortage, Ingredient ingredient) {
        return new StockShortageItemResponse(
                shortage.getStockShortagePublicId(),
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                shortage.getRequiredAmount(),
                shortage.getShortageAmount()
        );
    }
}
