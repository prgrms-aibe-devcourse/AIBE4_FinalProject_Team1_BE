package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.service.command.ShortageRelatedOrderQueryResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShortageRelatedOrderToolResponse(
        ShortageSummary shortage,
        OrderSummary order
) {

    public static ShortageRelatedOrderToolResponse from(ShortageRelatedOrderQueryResult result) {
        return new ShortageRelatedOrderToolResponse(
                ShortageSummary.from(result),
                OrderSummary.from(result)
        );
    }

    public record ShortageSummary(
            UUID shortagePublicId,
            String status,
            OffsetDateTime occurredAt,
            OffsetDateTime closedAt,
            UUID ingredientPublicId,
            String ingredientName,
            BigDecimal requiredAmount,
            BigDecimal shortageAmount
    ) {
        public static ShortageSummary from(ShortageRelatedOrderQueryResult result) {
            return new ShortageSummary(
                    result.shortagePublicId(),
                    result.shortageStatus(),
                    result.shortageOccurredAt(),
                    result.shortageClosedAt(),
                    result.ingredientPublicId(),
                    result.ingredientName(),
                    result.requiredAmount(),
                    result.shortageAmount()
            );
        }
    }

    public record OrderSummary(
            UUID orderPublicId,
            OffsetDateTime orderedAt,
            OffsetDateTime completedAt,
            String status,
            String type,
            BigDecimal totalAmount
    ) {
        public static OrderSummary from(ShortageRelatedOrderQueryResult result) {
            return new OrderSummary(
                    result.orderPublicId(),
                    result.orderedAt(),
                    result.completedAt(),
                    result.orderStatus(),
                    result.orderType(),
                    result.totalAmount()
            );
        }
    }
}