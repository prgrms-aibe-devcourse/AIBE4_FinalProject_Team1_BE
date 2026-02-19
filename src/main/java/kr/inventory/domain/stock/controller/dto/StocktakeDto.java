package kr.inventory.domain.stock.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public class StocktakeDto {
    public record CreateRequest(
            @NotEmpty(message = "전표 제목은 필수입니다.")
            String title,
            @Valid @NotEmpty(message = "실사 항목은 최소 1개 이상이어야 합니다.")
            List<ItemRequest> items
    ) {}

    public record ItemRequest(
            @NotNull Long ingredientId,
            @NotNull @PositiveOrZero BigDecimal stocktakeQty
    ) {}

    public record SheetResponse(
            Long sheetId,
            String title,
            String status
    ) {}
}