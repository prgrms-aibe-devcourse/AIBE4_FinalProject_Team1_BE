package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 입고 목록 조회 전용 요약 응답.
 * - 목록에서는 items(라인 상세)를 내려주지 않는다.
 */
public record StockInboundListResponse(
        UUID inboundPublicId,
        String storeName,
        Long vendorId,
        String vendorName,
        InboundStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        long itemCount,
        long unresolvedItemCount,
        BigDecimal totalCost
) {

    public static StockInboundListResponse from(
            StockInbound inbound,
            long itemCount,
            long unresolvedItemCount,
            BigDecimal totalCost
    ) {
        Vendor vendor = inbound.getVendor();
        return new StockInboundListResponse(
                inbound.getInboundPublicId(),
                inbound.getStore().getName(),
                vendor != null ? vendor.getVendorId() : null,
                vendor != null ? vendor.getName() : null,
                inbound.getStatus(),
                inbound.getCreatedAt(),
                inbound.getConfirmedAt(),
                itemCount,
                unresolvedItemCount,
                totalCost
        );
    }
}
