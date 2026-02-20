package kr.inventory.domain.purchase.factory;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.store.entity.Store;

import java.util.List;

public interface PurchaseOrderFactory {
    PurchaseOrder createDraft(Store store, List<PurchaseOrderItemRequest> itemRequests);

    List<PurchaseOrderItem> createItems(List<PurchaseOrderItemRequest> itemRequests);
}
