package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.entity.enums.StockTakeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockTakeSheetResponse(
        UUID sheetPublicId,
        String title,
        StockTakeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt
) {
    public static StockTakeSheetResponse from(StockTakeSheet sheet) {
        return new StockTakeSheetResponse(
                sheet.getSheetPublicId(),
                sheet.getTitle(),
                sheet.getStatus(),
                sheet.getCreatedAt(),
                sheet.getConfirmedAt()
        );
    }
}