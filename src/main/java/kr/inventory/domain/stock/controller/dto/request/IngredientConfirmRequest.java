package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record IngredientConfirmRequest(
    @NotNull(message = "선택한 재료 ID는 필수입니다.")
    UUID chosenIngredientPublicId
) {}
