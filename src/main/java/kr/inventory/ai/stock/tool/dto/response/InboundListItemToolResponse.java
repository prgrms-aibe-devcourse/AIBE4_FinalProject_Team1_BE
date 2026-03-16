package kr.inventory.ai.stock.tool.dto.response;

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
}