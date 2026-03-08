package kr.inventory.domain.stock.controller.dto.request;

import java.time.OffsetDateTime;

public record StockShortageSearchRequest(
        OffsetDateTime from,
        OffsetDateTime to
) {
}
