package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
public interface PurchaseOrderPdfService {
    byte[] generate(PurchaseOrder purchaseOrder);
}
