package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.entity.enums.StockTakeStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockTakeDetailResponse(
        UUID sheetPublicId,
        String title,
        StockTakeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        List<StockTakeItemResponse> items
) {
    public static StockTakeDetailResponse from(StockTakeSheet sheet, List<StockTakeItemResponse> items) {
        return new StockTakeDetailResponse(
                sheet.getSheetPublicId(),
                sheet.getTitle(),
                sheet.getStatus(),
                sheet.getCreatedAt(),
                sheet.getConfirmedAt(),
                items
        );
    }
}