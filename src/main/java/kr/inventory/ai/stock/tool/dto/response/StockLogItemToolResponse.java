package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.analytics.controller.dto.response.StockLogAnalyticResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockLogItemToolResponse(
        String ingredientName,
        String productDisplayName,

        String transactionType,
        String referenceType,

        BigDecimal changeQuantity,
        BigDecimal balanceAfter,

        OffsetDateTime createdAt
) {

    public static StockLogItemToolResponse from(StockLogAnalyticResponse response) {
        return new StockLogItemToolResponse(
                response.ingredientName(),
                response.productDisplayName(),
                response.transactionType(),
                response.referenceType(),
                response.changeQuantity(),
                response.balanceAfter(),
                response.createdAt()
        );
    }
}
