package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.InboundStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockInboundResponse(
	Long inboundId,
	UUID inboundPublicId,
	Long storeId,
	String storeName,
	Long vendorId,
	String vendorName,
	Long sourceDocumentId,
	Long sourcePurchaseOrderId,
	InboundStatus status,
	Long confirmedByUserId,
	String confirmedByUserName,
	OffsetDateTime confirmedAt,
	List<StockInboundItemResponse> items
) {

	public static StockInboundResponse from(StockInbound inbound, List<StockInboundItemResponse> items) {
		return createResponse(inbound, items);
	}

	// 입고 확정 로직용
	public static StockInboundResponse fromEntity(StockInbound inbound, List<StockInboundItem> items) {
		List<StockInboundItemResponse> itemResponses = items.stream()
			.map(StockInboundItemResponse::from)
			.toList();
		return createResponse(inbound, itemResponses);
	}

	private static StockInboundResponse createResponse(StockInbound inbound, List<StockInboundItemResponse> items) {
		return new StockInboundResponse(
			inbound.getInboundId(),
			inbound.getInboundPublicId(),
			inbound.getStore().getStoreId(),
			inbound.getStore().getName(),
			inbound.getVendor() != null ? inbound.getVendor().getVendorId() : null,
			inbound.getVendor() != null ? inbound.getVendor().getName() : null,
			inbound.getSourceDocument() != null ? inbound.getSourceDocument().getDocumentId() : null,
			inbound.getSourcePurchaseOrder() != null ? inbound.getSourcePurchaseOrder().getPurchaseOrderId() : null,
			inbound.getStatus(),
			inbound.getConfirmedByUser() != null ? inbound.getConfirmedByUser().getUserId() : null,
			inbound.getConfirmedByUser() != null ? inbound.getConfirmedByUser().getName() : null,
			inbound.getConfirmedAt(),
			items
		);
	}
}
