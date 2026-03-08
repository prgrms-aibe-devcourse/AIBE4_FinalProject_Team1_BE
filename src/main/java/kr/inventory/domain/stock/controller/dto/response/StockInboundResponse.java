package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.vendor.entity.Vendor;

import java.time.LocalDate;
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
    LocalDate inboundDate,
    Long confirmedByUserId,
    String confirmedByUserName,
    OffsetDateTime confirmedAt,
    List<StockInboundItemResponse> items
) {

    public static StockInboundResponse from(StockInbound inbound, List<StockInboundItemResponse> items) {
        Vendor vendor = inbound.getVendor();
        Document sourceDocument = inbound.getSourceDocument();
        PurchaseOrder sourcePurchaseOrder = inbound.getSourcePurchaseOrder();
        User confirmedByUser = inbound.getConfirmedByUser();

        return new StockInboundResponse(
            inbound.getInboundId(),
            inbound.getInboundPublicId(),
            inbound.getStore().getStoreId(),
            inbound.getStore().getName(),
            vendor != null ? vendor.getVendorId() : null,
            vendor != null ? vendor.getName() : null,
            sourceDocument != null ? sourceDocument.getDocumentId() : null,
            sourcePurchaseOrder != null ? sourcePurchaseOrder.getPurchaseOrderId() : null,
            inbound.getStatus(),
            inbound.getInboundDate(),
            confirmedByUser != null ? confirmedByUser.getUserId() : null,
            confirmedByUser != null ? confirmedByUser.getName() : null,
            inbound.getConfirmedAt(),
            items
        );
    }
}
