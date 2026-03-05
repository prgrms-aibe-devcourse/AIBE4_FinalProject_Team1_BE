package kr.inventory.domain.stock.controller.dto.response;

import java.util.List;

public record BulkIngredientConfirmResponse(
        int totalCount,
        int successCount,
        int failedCount,
        List<BulkIngredientConfirmItemResultResponse> results
) {
    public static BulkIngredientConfirmResponse from(
            int totalCount,
            int successCount,
            int failedCount,
            List<BulkIngredientConfirmItemResultResponse> results
    ) {
        return new BulkIngredientConfirmResponse(totalCount, successCount, failedCount, results);
    }
}