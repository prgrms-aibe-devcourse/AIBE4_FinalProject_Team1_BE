package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SalesOrderDetailToolResponse(
        String actionKey,
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
        List<SalesOrderDetailItemToolResponse> items,
        List<SuggestedAction> suggestedFollowUps
) {
    public static SalesOrderDetailToolResponse from(
            SalesLedgerOrderDetailResponse response,
            List<SuggestedAction> suggestedFollowUps
    ) {
        return new SalesOrderDetailToolResponse(
                "sales.order_detail",
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
                response.netAmount(),
                response.items().stream()
                        .map(SalesOrderDetailItemToolResponse::from)
                        .toList(),
                suggestedFollowUps
        );
    }
}
