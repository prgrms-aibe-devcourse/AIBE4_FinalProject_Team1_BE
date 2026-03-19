package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderSummaryResponse;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SalesRecordItemToolResponse(
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
    public static SalesRecordItemToolResponse from(SalesLedgerOrderSummaryResponse response) {
        return new SalesRecordItemToolResponse(
                response.orderPublicId(),
                response.status(),
                response.type(),
                response.orderedAt(),
                response.completedAt(),
                response.refundedAt(),
                response.tableCode(),
                response.itemCount(),
                response.totalAmount(),
                response.refundAmount(),
                response.netAmount()
        );
    }
}
