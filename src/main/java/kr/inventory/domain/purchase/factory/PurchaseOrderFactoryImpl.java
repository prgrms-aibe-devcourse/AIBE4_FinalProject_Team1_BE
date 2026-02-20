package kr.inventory.domain.purchase.factory;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.store.entity.Store;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseOrderFactoryImpl implements PurchaseOrderFactory {

    @Override
    public PurchaseOrder createDraft(Store store, List<PurchaseOrderItemRequest> itemRequests) {
        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(store);
        purchaseOrder.replaceItems(createItems(itemRequests));
        return purchaseOrder;
    }

    @Override
    public List<PurchaseOrderItem> createItems(List<PurchaseOrderItemRequest> itemRequests) {
        return itemRequests.stream()
                .map(itemRequest -> PurchaseOrderItem.create(itemRequest.itemName(), itemRequest.quantity(), itemRequest.unitPrice()))
                .toList();
    }
}
