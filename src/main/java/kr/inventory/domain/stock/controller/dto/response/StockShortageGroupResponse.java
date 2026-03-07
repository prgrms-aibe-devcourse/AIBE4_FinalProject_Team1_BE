package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.sales.entity.SalesOrder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockShortageGroupResponse(
        UUID salesOrderPublicId,
        List<StockShortageItemResponse> shortages,
        OffsetDateTime createdAt
) {
    public static StockShortageGroupResponse from(
            SalesOrder salesOrder,
            List<StockShortageItemResponse> shortages
    ) {
        return new StockShortageGroupResponse(
                salesOrder.getOrderPublicId(),
                shortages,
                salesOrder.getCreatedAt()
        );
    }
}
