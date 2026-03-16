package kr.inventory.ai.stock.tool.dto.response;

import java.util.List;

public record InboundListToolResponse(
        int count,
        List<InboundListItemToolResponse> inbounds
) {
}