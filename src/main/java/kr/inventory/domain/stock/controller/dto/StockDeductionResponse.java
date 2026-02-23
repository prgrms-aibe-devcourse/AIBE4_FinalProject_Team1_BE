package kr.inventory.domain.stock.controller.dto;

public record StockDeductionResponse(
        Long salesOrderId,
        String message
) {
    public static StockDeductionResponse from(Long salesOrderId, String message) {
        return new StockDeductionResponse(
                salesOrderId,
                message
        );
    }
}