package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record StockTakeItemQuantityRequest(
        @NotNull(message = "ingredientPublicId는 필수입니다.")
        UUID ingredientPublicId,

        @NotNull(message = "stockTakeQty는 필수입니다.")
        @PositiveOrZero(message = "stockTakeQty는 0 이상이어야 합니다.")
        BigDecimal stockTakeQty
) {
}