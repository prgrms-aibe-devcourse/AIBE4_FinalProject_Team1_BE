package kr.inventory.domain.stock.normalization.model;

import java.util.UUID;

public record BulkIngredientConfirmItemCommand(
        UUID inboundItemPublicId,
        UUID chosenIngredientPublicId
) {
}