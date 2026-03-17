package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.service.command.StockInboundSummary;

import java.util.List;

public record InboundListToolResponse(
        int count,
        List<InboundListItemToolResponse> inbounds
) {
    public static InboundListToolResponse from(List<StockInboundSummary> summaries) {
        List<InboundListItemToolResponse> inbounds = summaries.stream()
                .map(InboundListItemToolResponse::from)
                .toList();

        return new InboundListToolResponse(
                inbounds.size(),
                inbounds
        );
    }
}