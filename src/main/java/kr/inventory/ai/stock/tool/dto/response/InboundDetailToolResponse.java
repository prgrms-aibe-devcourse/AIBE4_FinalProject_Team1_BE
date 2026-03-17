package kr.inventory.ai.stock.tool.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InboundDetailToolResponse(
        String message,
        UUID inboundPublicId,
        LocalDate inboundDate,
        OffsetDateTime createdAt,
        String vendorName,
        String status,
        BigDecimal totalCost,
        List<InboundDetailItemToolResponse> items
) {
}
