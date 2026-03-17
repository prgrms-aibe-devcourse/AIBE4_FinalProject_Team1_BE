package kr.inventory.domain.stock.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockInboundDetailResult(
        UUID inboundPublicId,
        LocalDate inboundDate,
        OffsetDateTime createdAt,
        String vendorName,
        String status,
        List<StockInboundItemDetail> items
) {
    public record StockInboundItemDetail(
            UUID inboundItemPublicId,
            String rawProductName,
            String mappedIngredientName,
            BigDecimal quantity,
            BigDecimal unitCost,
            LocalDate expirationDate,
            String resolutionStatus
    ) {
    }
}
