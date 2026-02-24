package kr.inventory.domain.stock.controller.dto;

import kr.inventory.domain.stock.entity.StockTakeSheet;

public record StockTakeSheetResponse(
        Long sheetId,
        String title,
        String status
) {
    public static StockTakeSheetResponse from(StockTakeSheet stocktakeSheet) {
        return new StockTakeSheetResponse(
                stocktakeSheet.getSheetId(),
                stocktakeSheet.getTitle(),
                stocktakeSheet.getStatus().name()
        );
    }
}