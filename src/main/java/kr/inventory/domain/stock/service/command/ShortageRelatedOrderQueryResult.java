package kr.inventory.domain.stock.service.command;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShortageRelatedOrderQueryResult(
        UUID shortagePublicId,
        String shortageStatus,
        OffsetDateTime shortageOccurredAt,
        OffsetDateTime shortageClosedAt,
        UUID ingredientPublicId,
        String ingredientName,
        BigDecimal requiredAmount,
        BigDecimal shortageAmount,
        UUID orderPublicId,
        OffsetDateTime orderedAt,
        OffsetDateTime completedAt,
        String orderStatus,
        String orderType,
        BigDecimal totalAmount
) {
}