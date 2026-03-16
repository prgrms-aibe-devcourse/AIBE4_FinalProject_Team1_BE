package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.stock.entity.enums.InboundStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockInboundSummary(
        UUID inboundPublicId,
        LocalDate inboundDate,
        InboundStatus status,
        String vendorName,
        int itemCount,
        String confirmedByName,
        OffsetDateTime confirmedAt,
        boolean hasItemsNeedingNormalization
) {
    public boolean isConfirmed() {
        return status == InboundStatus.CONFIRMED;
    }
}
