package kr.inventory.domain.stock.controller.dto.request;

import java.util.UUID;

// 입고 아이템 재료 매핑 확정 요청 -> 기존 재료 요청 or 새 재료 생성
public record IngredientConfirmRequest(
    UUID existingIngredientPublicId,

    String newIngredientName,

    String newIngredientUnit,

    String specText
) {
}
