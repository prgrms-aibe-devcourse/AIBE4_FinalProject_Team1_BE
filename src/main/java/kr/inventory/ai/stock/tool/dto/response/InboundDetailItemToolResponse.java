package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.service.command.StockInboundDetailResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InboundDetailItemToolResponse(
        UUID inboundItemPublicId,
        String rawProductName,
        String mappedIngredientName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal amount,
        LocalDate expirationDate,
        String resolutionStatus
) {
    public static InboundDetailItemToolResponse from(StockInboundDetailResult.StockInboundItemDetail item) {
        BigDecimal amount = null;
        if (item.quantity() != null && item.unitCost() != null) {
            amount = item.quantity().multiply(item.unitCost());
        }

        return new InboundDetailItemToolResponse(
                item.inboundItemPublicId(),
                item.rawProductName(),
                item.mappedIngredientName(),
                item.quantity(),
                item.unitCost(),
                amount,
                item.expirationDate(),
                item.resolutionStatus()
        );
    }
}