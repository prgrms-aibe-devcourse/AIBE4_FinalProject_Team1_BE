package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.enums.ResolutionStatus;

import java.util.UUID;

// 개별 입고 아이템 정규화 결과
public record ItemResolveResult(
    UUID inboundItemPublicId,
    String rawProductName,
    String normalizedRawKey,
    String normalizedRawFull,
    ResolutionStatus resolutionStatus,
    UUID ingredientPublicId,
    String ingredientName
) {
}
