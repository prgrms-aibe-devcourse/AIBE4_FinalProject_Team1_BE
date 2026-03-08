package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockInboundListResponse(
        UUID inboundPublicId,
        String storeName,
        UUID vendorPublicId,
        String vendorName,
        InboundStatus status,
        LocalDate inboundDate,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        long itemCount,
        BigDecimal totalCost
) {

    public static StockInboundListResponse from(
            StockInbound inbound,
            long itemCount,
            BigDecimal totalCost
    ) {
        Vendor vendor = inbound.getVendor();
        return new StockInboundListResponse(
                inbound.getInboundPublicId(),
                inbound.getStore().getName(),
                vendor != null ? vendor.getVendorPublicId() : null,
                vendor != null ? vendor.getName() : null,
                inbound.getStatus(),
                inbound.getInboundDate(),
                inbound.getCreatedAt(),
                inbound.getConfirmedAt(),
                itemCount,
                totalCost
        );
    }
}
