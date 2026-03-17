package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.stock.entity.StockInboundItem;

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
        public static StockInboundItemDetail toItemDetail(StockInboundItem item) {
            return new StockInboundItemDetail(
                    item.getInboundItemPublicId(),
                    item.getRawProductName(),
                    item.getIngredient() != null ? item.getIngredient().getName() : null,
                    item.getQuantity(),
                    item.getUnitCost(),
                    item.getExpirationDate(),
                    item.getResolutionStatus() != null ? item.getResolutionStatus().name() : null
            );
        }
    }
}
