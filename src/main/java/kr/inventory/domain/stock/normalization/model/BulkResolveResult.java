package kr.inventory.domain.stock.normalization.model;

// 입고 전체 자동 매핑 결과
public record BulkResolveResult(
    int totalCount,
    int autoResolvedCount,
    int pendingCount,
    int failedCount,
    int skippedCount
) {
}
