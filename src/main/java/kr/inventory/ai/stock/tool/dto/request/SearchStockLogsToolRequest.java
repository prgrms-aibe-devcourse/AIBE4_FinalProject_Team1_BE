package kr.inventory.ai.stock.tool.dto.request;

import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

public record SearchStockLogsToolRequest(
        String keyword,
        String transactionType,
        String referenceType,
        OffsetDateTime from,
        OffsetDateTime to,
        Integer limit
) {

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public String normalizedTransactionType() {
        return StringUtils.hasText(transactionType) ? transactionType.trim() : null;
    }

    public String normalizedReferenceType() {
        return StringUtils.hasText(referenceType) ? referenceType.trim() : null;
    }

    public int resolvedLimit() {
        if (limit == null || limit <= 0) {
            return 10;
        }
        return Math.min(limit, 20);
    }
}
