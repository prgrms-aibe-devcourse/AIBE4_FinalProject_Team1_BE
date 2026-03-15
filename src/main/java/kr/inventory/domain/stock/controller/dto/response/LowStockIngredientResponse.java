package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockIngredientResponse(
        UUID ingredientPublicId,
        String ingredientName,
        BigDecimal currentQuantity,
        BigDecimal thresholdQuantity
) {
}
