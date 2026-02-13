package kr.inventory.domain.stock.controller.dto;

import jakarta.validation.constraints.NotNull;

public class StockRequestDto {
    public record OrderDeductionRequest(
            @NotNull Long salesOrderId
    ) {}

    public record DeductionResponse(
            Long salesOrderId,
            String status,
            String message
    ) {}
}