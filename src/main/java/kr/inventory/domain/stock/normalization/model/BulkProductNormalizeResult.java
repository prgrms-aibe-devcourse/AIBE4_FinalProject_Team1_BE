package kr.inventory.domain.stock.normalization.model;

// 입고 전체 상품명 정규화 처리 결과
public record BulkProductNormalizeResult(
        int totalCount,
        int normalizedCount,
        int skippedCount,
        int failedCount
) {
}
