package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.service.command.StockInboundSummary;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InboundListItemToolResponse(
        UUID inboundPublicId,
        LocalDate inboundDate,
        String vendorName,
        int itemCount,
        String confirmedByName,
        OffsetDateTime confirmedAt
) {
    public static InboundListItemToolResponse from(StockInboundSummary summary) {
        return new InboundListItemToolResponse(
                summary.inboundPublicId(),
                summary.inboundDate(),
                summary.vendorName(),
                summary.itemCount(),
                summary.confirmedByName(),
                summary.confirmedAt()
        );
    }
}