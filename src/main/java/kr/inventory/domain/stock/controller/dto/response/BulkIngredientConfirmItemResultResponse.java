package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.normalization.model.ConfirmResult;

import java.util.UUID;

public record BulkIngredientConfirmItemResultResponse(
        UUID inboundItemPublicId,
        boolean success,
        UUID confirmedIngredientPublicId,
        String ingredientName,
        String normalizedRawKey,
        boolean newMappingCreated,
        String errorMessage
) {
    public static BulkIngredientConfirmItemResultResponse success(UUID inboundItemPublicId, ConfirmResult confirmResult) {
        return new BulkIngredientConfirmItemResultResponse(
                inboundItemPublicId,
                true,
                confirmResult.ingredientPublicId(),
                confirmResult.ingredientName(),
                confirmResult.normalizedRawKey(),
                confirmResult.newMappingCreated(),
                null
        );
    }

    public static BulkIngredientConfirmItemResultResponse failure(UUID inboundItemPublicId, String errorMessage) {
        return new BulkIngredientConfirmItemResultResponse(
                inboundItemPublicId,
                false,
                null,
                null,
                null,
                false,
                errorMessage
        );
    }
}