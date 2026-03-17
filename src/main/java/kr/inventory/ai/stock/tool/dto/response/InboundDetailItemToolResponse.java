package kr.inventory.ai.stock.tool.dto.response;

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
}