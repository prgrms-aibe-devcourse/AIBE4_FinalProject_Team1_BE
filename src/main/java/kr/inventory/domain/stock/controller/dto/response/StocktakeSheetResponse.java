package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StocktakeSheet;

public record StocktakeSheetResponse(
	Long sheetId,
	String title,
	String status
) {
	public static StocktakeSheetResponse from(StocktakeSheet stocktakeSheet) {
		return new StocktakeSheetResponse(
			stocktakeSheet.getSheetId(),
			stocktakeSheet.getTitle(),
			stocktakeSheet.getStatus().name()
		);
	}
}