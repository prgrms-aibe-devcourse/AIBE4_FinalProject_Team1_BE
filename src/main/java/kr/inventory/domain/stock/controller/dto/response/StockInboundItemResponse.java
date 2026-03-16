package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockInboundItemResponse(
        Long inboundItemId,
        UUID inboundItemPublicId,
        Long inboundId,
        Long ingredientId,
        String ingredientName,
        String rawProductName,
        BigDecimal quantity,
        BigDecimal normalizedQuantity,
        BigDecimal unitCost,
        LocalDate expirationDate,
        ResolutionStatus resolutionStatus,
        String normalizedRawKey,
        String normalizedRawFull,
        String specText,
        String productDisplayName
) {
    public static StockInboundItemResponse from(StockInboundItem item) {
        Ingredient ingredient = item.getIngredient();

        return new StockInboundItemResponse(
                item.getInboundItemId(),
                item.getInboundItemPublicId(),
                item.getInbound().getInboundId(),
                ingredient != null ? ingredient.getIngredientId() : null,
                ingredient != null ? ingredient.getName() : null,
                item.getRawProductName(),
                item.getQuantity(),
                item.getEffectiveQuantity(),
                item.getUnitCost(),
                item.getExpirationDate(),
                item.getResolutionStatus(),
                item.getNormalizedRawKey(),
                item.getNormalizedRawFull(),
                item.getSpecText(),
                item.getProductDisplayName()
        );
    }
}