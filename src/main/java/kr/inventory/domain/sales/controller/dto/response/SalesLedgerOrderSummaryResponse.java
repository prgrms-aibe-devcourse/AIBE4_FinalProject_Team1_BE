package kr.inventory.domain.sales.controller.dto.response;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SalesLedgerOrderSummaryResponse(
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
        BigDecimal netAmount
) {
    public static SalesLedgerOrderSummaryResponse from(
            SalesOrder order,
            Integer itemCount,
            BigDecimal refundAmount,
            BigDecimal netAmount
    ) {
        return new SalesLedgerOrderSummaryResponse(
                order.getOrderPublicId(),
                order.getStatus(),
                order.getType(),
                order.getOrderedAt(),
                order.getCompletedAt(),
                order.getRefundedAt(),
                order.getDiningTable().getTableCode(),
                itemCount,
                order.getTotalAmount(),
                refundAmount,
                netAmount
        );
    }
}
