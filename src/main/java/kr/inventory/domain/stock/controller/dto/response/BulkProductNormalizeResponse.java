package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.normalization.model.BulkProductNormalizeResult;

// 입고 전체 상품명 정규화 결과 응답
public record BulkProductNormalizeResponse(
        int totalCount,
        int normalizedCount,
        int skippedCount,
        int failedCount
) {
    public static BulkProductNormalizeResponse from(BulkProductNormalizeResult result) {
        return new BulkProductNormalizeResponse(
                result.totalCount(),
                result.normalizedCount(),
                result.skippedCount(),
                result.failedCount()
        );
    }
}
