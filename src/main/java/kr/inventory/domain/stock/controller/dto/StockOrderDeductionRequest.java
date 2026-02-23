package kr.inventory.domain.stock.controller.dto;

import jakarta.validation.constraints.NotNull;

public record StockOrderDeductionRequest(
        @NotNull Long storeId,
        @NotNull Long salesOrderId
) {}
