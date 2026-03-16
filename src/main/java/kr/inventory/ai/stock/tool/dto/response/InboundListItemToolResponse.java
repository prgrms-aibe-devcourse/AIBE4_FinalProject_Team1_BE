package kr.inventory.ai.stock.tool.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InboundListItemToolResponse(
        UUID inboundPublicId,
        LocalDate inboundDate,
        String status,
        String vendorName,
        int itemCount,
        boolean confirmed,
        String confirmedByName,
        OffsetDateTime confirmedAt,
        boolean hasItemsNeedingNormalization,
        List<String> availableActions
) {
}