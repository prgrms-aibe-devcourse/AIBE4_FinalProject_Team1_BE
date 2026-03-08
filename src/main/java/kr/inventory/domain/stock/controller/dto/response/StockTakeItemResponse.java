package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockTake;

import java.math.BigDecimal;
import java.util.UUID;

public record StockTakeItemResponse(
        UUID ingredientPublicId,
        String ingredientName,
        String unit,
        BigDecimal stockTakeQty,
        BigDecimal theoreticalQty,
        BigDecimal varianceQty
) {
    public static StockTakeItemResponse from(StockTake item) {
        return new StockTakeItemResponse(
                item.getIngredient().getIngredientPublicId(),
                item.getIngredientName(),
                item.getUnit().name(),
                item.getStockTakeQty(),
                item.getTheoreticalQty(),
                item.getVarianceQty()
        );
    }
}