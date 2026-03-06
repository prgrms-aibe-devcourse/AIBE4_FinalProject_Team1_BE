package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.normalization.model.ConfirmResult;

import java.util.UUID;

public record IngredientConfirmResponse(
    UUID inboundItemPublicId,
    UUID ingredientPublicId,
    String ingredientName,
    String normalizedRawKey,
    boolean newMappingCreated
) {

    public static IngredientConfirmResponse from(ConfirmResult result) {
        return new IngredientConfirmResponse(
            result.inboundItemPublicId(),
            result.ingredientPublicId(),
            result.ingredientName(),
            result.normalizedRawKey(),
            result.newMappingCreated()
        );
    }
}
