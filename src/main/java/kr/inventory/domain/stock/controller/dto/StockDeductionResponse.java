package kr.inventory.domain.stock.controller.dto;

public record StockDeductionResponse(
        Long salesOrderId,
        String status,
        String message
) {}