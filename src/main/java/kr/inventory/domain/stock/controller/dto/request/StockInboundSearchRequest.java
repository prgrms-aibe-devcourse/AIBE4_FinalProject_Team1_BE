package kr.inventory.domain.stock.controller.dto.request;

import java.time.LocalDate;

public record StockInboundSearchRequest(
        String vendorName,
        String itemKeyword,
        String inboundPublicId,
        LocalDate inboundDateFrom,
        LocalDate inboundDateTo
) {
}