package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.normalization.model.BulkResolveResult;

// 입고 전체 자동 매핑 결과 요약
public record BulkResolveResponse(
    int totalCount,
    int autoResolvedCount,
    int pendingCount,
    int failedCount,
    int skippedCount
) {
    public static BulkResolveResponse from(BulkResolveResult result) {
        return new BulkResolveResponse(
            result.totalCount(),
            result.autoResolvedCount(),
            result.pendingCount(),
            result.failedCount(),
            result.skippedCount()
        );
    }
}
