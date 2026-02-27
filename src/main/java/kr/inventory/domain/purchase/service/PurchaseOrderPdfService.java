package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.entity.PurchaseOrder;

public interface PurchaseOrderPdfService {

    /** 발주서 정보를 PDF 바이트 배열로 생성한다. */
    byte[] generate(PurchaseOrder purchaseOrder);
}
