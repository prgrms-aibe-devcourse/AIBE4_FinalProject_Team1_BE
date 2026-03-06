package kr.inventory.domain.stock.normalization.model;

import java.util.List;

public record BulkConfirmResult(
        int totalCount,
        int successCount,
        int failedCount,
        List<BulkConfirmItemResultDetail> results
) {
}