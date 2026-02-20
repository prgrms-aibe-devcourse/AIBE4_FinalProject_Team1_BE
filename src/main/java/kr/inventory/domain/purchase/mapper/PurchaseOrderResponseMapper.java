package kr.inventory.domain.purchase.mapper;

import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderItemResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderResponseMapper {

    public PurchaseOrderSummaryResponse toSummaryResponse(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderSummaryResponse(
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getStore().getStoreId(),
                purchaseOrder.getOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getSubmittedAt()
        );
    }

    public PurchaseOrderDetailResponse toDetailResponse(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderDetailResponse(
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getStore().getStoreId(),
                purchaseOrder.getOrderNo(),
                purchaseOrder.getStatus(),
                purchaseOrder.getTotalAmount(),
                purchaseOrder.getSubmittedByUserId(),
                purchaseOrder.getSubmittedAt(),
                purchaseOrder.getConfirmedByUserId(),
                purchaseOrder.getConfirmedAt(),
                purchaseOrder.getCanceledByUserId(),
                purchaseOrder.getCanceledAt(),
                purchaseOrder.getItems().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getItemName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineAmount()
        );
    }
}
