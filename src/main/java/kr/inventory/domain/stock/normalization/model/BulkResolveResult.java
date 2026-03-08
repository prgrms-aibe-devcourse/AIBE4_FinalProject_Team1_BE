package kr.inventory.domain.stock.normalization.model;

import kr.inventory.domain.stock.controller.dto.response.ItemResolveResult;

import java.util.List;

// 입고 전체 자동 매핑 결과
public record BulkResolveResult(
    int totalCount,
    int autoResolvedCount,
    int failedCount,
    int skippedCount,
    List<ItemResolveResult> items
) {
}
