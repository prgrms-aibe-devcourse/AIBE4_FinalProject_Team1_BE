package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record StockShortageItemResponse(
        UUID stockShortagePublicId,
        UUID ingredientPublicId,
        String ingredientName,
        String unit,
        BigDecimal requiredAmount,
        BigDecimal shortageAmount,
        ShortageStatus status
) {
    public static StockShortageItemResponse from(StockShortage shortage, Ingredient ingredient) {
        return new StockShortageItemResponse(
                shortage.getStockShortagePublicId(),
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                ingredient.getUnit().name(),
                shortage.getRequiredAmount(),
                shortage.getShortageAmount(),
                shortage.getStatus()
        );
    }
}
