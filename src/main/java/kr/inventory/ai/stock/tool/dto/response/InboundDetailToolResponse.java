package kr.inventory.ai.stock.tool.dto.response;

import kr.inventory.domain.stock.service.command.StockInboundDetailResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InboundDetailToolResponse(
        String message,
        UUID inboundPublicId,
        LocalDate inboundDate,
        OffsetDateTime createdAt,
        String vendorName,
        String status,
        BigDecimal totalCost,
        List<InboundDetailItemToolResponse> items
) {
    public static InboundDetailToolResponse from(StockInboundDetailResult detail) {
        List<InboundDetailItemToolResponse> items = detail.items().stream()
                .map(InboundDetailItemToolResponse::from)
                .toList();

        BigDecimal totalCost = items.stream()
                .map(InboundDetailItemToolResponse::amount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String vendorName = detail.vendorName() != null ? detail.vendorName() : "거래처 정보 없음";

        String message = "%s / %s / %s 입고의 상세 정보입니다."
                .formatted(
                        detail.inboundPublicId(),
                        detail.createdAt(),
                        vendorName
                );

        return new InboundDetailToolResponse(
                message,
                detail.inboundPublicId(),
                detail.inboundDate(),
                detail.createdAt(),
                vendorName,
                detail.status(),
                totalCost,
                items
        );
    }
}
