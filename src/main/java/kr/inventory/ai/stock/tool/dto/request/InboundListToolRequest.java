package kr.inventory.ai.stock.tool.dto.request;

import kr.inventory.domain.stock.entity.enums.InboundStatus;

public record InboundListToolRequest(
    InboundStatus status,
    String keyword,
    Integer limit
) {
    public int resolvedLimit(){
        if(limit == null || limit <=0){
            return 10;
        }
        return Math.min(limit,20);
    }

    public String normalizedKeyword() {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
