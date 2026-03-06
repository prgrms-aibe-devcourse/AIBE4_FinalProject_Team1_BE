package kr.inventory.domain.sales.controller.dto.response;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SalesLedgerOrderDetailResponse(
        UUID orderPublicId,
        SalesOrderStatus status,
        SalesOrderType type,
        OffsetDateTime orderedAt,
        OffsetDateTime completedAt,
        OffsetDateTime refundedAt,
        String tableCode,
        Integer itemCount,
        BigDecimal totalAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        List<SalesOrderItemResponse> items
) {
    public static SalesLedgerOrderDetailResponse from(
            SalesOrder order,
            List<SalesOrderItem> items,
            BigDecimal refundAmount,
            BigDecimal netAmount
    ) {
        return new SalesLedgerOrderDetailResponse(
                order.getOrderPublicId(),
                order.getStatus(),
                order.getType(),
                order.getOrderedAt(),
                order.getCompletedAt(),
                order.getRefundedAt(),
                order.getDiningTable().getTableCode(),
                items.size(),
                order.getTotalAmount(),
                refundAmount,
                netAmount,
                items.stream().map(SalesOrderItemResponse::from).toList()
        );
    }
}
