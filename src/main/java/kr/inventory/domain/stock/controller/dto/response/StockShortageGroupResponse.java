package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.sales.entity.SalesOrder;

import java.util.List;
import java.util.UUID;

public record StockShortageGroupResponse(
        UUID salesOrderPublicId,
        List<StockShortageItemResponse> shortages
) {
    public static StockShortageGroupResponse from(
            SalesOrder salesOrder,
            List<StockShortageItemResponse> shortages
    ) {
        return new StockShortageGroupResponse(
                salesOrder.getOrderPublicId(),
                shortages
        );
    }
}
