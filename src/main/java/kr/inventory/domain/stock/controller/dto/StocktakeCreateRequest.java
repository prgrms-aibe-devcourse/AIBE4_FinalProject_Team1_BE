package kr.inventory.domain.stock.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StocktakeCreateRequest(
        @NotEmpty(message = "전표 제목은 필수입니다.")
        String title,

        @Valid @NotEmpty(message = "실사 항목은 최소 1개 이상이어야 합니다.")
        List<StocktakeItemRequest> items
) {}
