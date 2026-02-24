package kr.inventory.domain.stock.controller.dto;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;

import java.time.OffsetDateTime;

public record StockInboundResponse(
	Long inboundId,
	Long storeId,
	String storeName,
	Long vendorId,
	String vendorName,
	Long sourceDocumentId,
	Long sourcePurchaseOrderId,
	InboundStatus status,
	Long confirmedByUserId,
	String confirmedByUserName,
	OffsetDateTime confirmedAt
) {
	public static StockInboundResponse from(StockInbound inbound) {
		return new StockInboundResponse(
			inbound.getInboundId(),
			inbound.getStore().getStoreId(),
			inbound.getStore().getName(),
			inbound.getVendor() != null ? inbound.getVendor().getVendorId() : null,
			inbound.getVendor() != null ? inbound.getVendor().getName() : null,
			inbound.getSourceDocument() != null ? inbound.getSourceDocument().getDocumentId() : null,
			inbound.getSourcePurchaseOrder() != null ? inbound.getSourcePurchaseOrder().getPurchaseOrderId() : null,
			inbound.getStatus(),
			inbound.getConfirmedByUser() != null ? inbound.getConfirmedByUser().getUserId() : null,
			inbound.getConfirmedByUser() != null ? inbound.getConfirmedByUser().getName() : null,
			inbound.getConfirmedAt()
		);
	}
}