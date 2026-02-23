package kr.inventory.domain.stock.controller.dto;

public record StocktakeSheetResponse(
        Long sheetId,
        String title,
        String status
) {}
