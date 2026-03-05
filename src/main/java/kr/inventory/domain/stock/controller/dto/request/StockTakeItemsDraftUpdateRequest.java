package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockTakeItemsDraftUpdateRequest(
        @NotEmpty(message = "수정할 항목 리스트는 비어 있을 수 없습니다.")
        List<@Valid StockTakeItemDraftUpdateRequest> items
) {}