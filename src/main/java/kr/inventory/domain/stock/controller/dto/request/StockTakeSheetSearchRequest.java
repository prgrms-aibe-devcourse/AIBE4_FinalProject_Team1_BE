package kr.inventory.domain.stock.controller.dto.request;

import java.time.OffsetDateTime;

public record StockTakeSheetSearchRequest(
        String title,
        OffsetDateTime from,
        OffsetDateTime to
) {
}
