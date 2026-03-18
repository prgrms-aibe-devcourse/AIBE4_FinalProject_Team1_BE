package kr.inventory.ai.stock.tool.dto.request;

import java.time.OffsetDateTime;

public record InboundListToolRequest(
    String keyword,
    Integer limit,
    OffsetDateTime from,
    OffsetDateTime to
) {
    public int resolvedLimit(){
        if(limit == null || limit <=0){
            return 30;
        }
        return Math.min(limit,30);
    }

    public String normalizedKeyword() {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
