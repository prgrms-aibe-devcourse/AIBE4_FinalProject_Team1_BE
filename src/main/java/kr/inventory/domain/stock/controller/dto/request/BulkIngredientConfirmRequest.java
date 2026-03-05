package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkIngredientConfirmRequest(
        @NotEmpty(message = "확정할 아이템 목록은 필수입니다.")
        @Valid
        List<BulkIngredientConfirmItemRequest> items
) {
}