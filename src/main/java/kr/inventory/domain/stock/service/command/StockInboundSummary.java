package kr.inventory.domain.stock.service.command;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockInboundSummary(
        UUID inboundPublicId,
        LocalDate inboundDate,
        String vendorName,
        int itemCount,
        String confirmedByName,
        OffsetDateTime confirmedAt
) {}
