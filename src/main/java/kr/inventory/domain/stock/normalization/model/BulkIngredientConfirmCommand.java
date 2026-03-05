package kr.inventory.domain.stock.normalization.model;

import kr.inventory.domain.stock.controller.dto.request.BulkIngredientConfirmItemRequest;
import kr.inventory.domain.stock.controller.dto.request.BulkIngredientConfirmRequest;

import java.util.ArrayList;
import java.util.List;

public record BulkIngredientConfirmCommand(
        List<BulkIngredientConfirmItemCommand> items
) {
    public static BulkIngredientConfirmCommand from(BulkIngredientConfirmRequest request) {
        List<BulkIngredientConfirmItemCommand> items = new ArrayList<>();

        if (request == null || request.items() == null || request.items().isEmpty()) {
            return new BulkIngredientConfirmCommand(items);
        }

        for (BulkIngredientConfirmItemRequest item : request.items()) {
            items.add(new BulkIngredientConfirmItemCommand(
                    item.inboundItemPublicId(),
                    item.chosenIngredientPublicId()
            ));
        }

        return new BulkIngredientConfirmCommand(items);
    }
}