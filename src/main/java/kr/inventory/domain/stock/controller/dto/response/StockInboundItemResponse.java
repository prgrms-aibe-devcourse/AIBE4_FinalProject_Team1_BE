package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;

public record StockInboundItemResponse(
    Long inboundItemId,
    UUID inboundItemPublicId,
    Long inboundId,
    Long ingredientId,
    String ingredientName,
    String rawProductName,
    BigDecimal quantity,
    BigDecimal unitCost,
    LocalDate expirationDate,
    ResolutionStatus resolutionStatus,
    String specText
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
            item.getUnitCost(),
            item.getExpirationDate(),
            item.getResolutionStatus(),
            item.getSpecText()
        );
    }
}
