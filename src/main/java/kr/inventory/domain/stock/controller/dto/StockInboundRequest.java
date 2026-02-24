package kr.inventory.domain.stock.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockInboundRequest(@NotNull Long storeId,
								  Long vendorId,
								  Long sourceDocumentId,
								  Long sourcePurchaseOrderId,
								  @NotEmpty @Valid List<StockInboundItemRequest> items) {
}
