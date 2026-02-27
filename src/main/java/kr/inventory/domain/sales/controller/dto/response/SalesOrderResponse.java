package kr.inventory.domain.sales.controller.dto.response;

import kr.inventory.domain.sales.controller.dto.response.SalesOrderItemResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SalesOrderResponse(
        UUID orderPublicId,
        SalesOrderStatus status,
        SalesOrderType type,
        BigDecimal totalAmount,
        OffsetDateTime orderedAt,
        OffsetDateTime completedAt,
        OffsetDateTime refundedAt,
        String tableCode,
        List<SalesOrderItemResponse> items
) {
    public static SalesOrderResponse from(SalesOrder order, List<SalesOrderItem> items) {
        return new SalesOrderResponse(
                order.getOrderPublicId(),
                order.getStatus(),
                order.getType(),
                order.getTotalAmount(),
                order.getOrderedAt(),
                order.getCompletedAt(),
                order.getRefundedAt(),
                order.getDiningTable().getTableCode(),
                items.stream()
                        .map(SalesOrderItemResponse::from)
                        .toList()
        );
    }
}