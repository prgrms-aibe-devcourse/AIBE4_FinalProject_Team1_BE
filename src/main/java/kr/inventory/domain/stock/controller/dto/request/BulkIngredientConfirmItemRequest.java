package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BulkIngredientConfirmItemRequest(
        @NotNull(message = "입고 아이템 ID는 필수입니다.")
        UUID inboundItemPublicId,

        @NotNull(message = "선택한 재료 ID는 필수입니다.")
        UUID chosenIngredientPublicId
) {
}