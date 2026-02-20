package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;

import java.util.List;

public interface PurchaseOrderService {
    /**
     * 발주서 초안을 생성한다.
     */
    PurchaseOrderDetailResponse createDraft(Long userId, PurchaseOrderCreateRequest request);

    /**
     * 매장 기준 발주서 목록을 조회한다.
     */
    List<PurchaseOrderSummaryResponse> getPurchaseOrders(Long userId, Long storeId);

    /**
     * 발주서 상세를 조회한다.
     */
    PurchaseOrderDetailResponse getPurchaseOrder(Long userId, Long purchaseOrderId);

    /**
     * DRAFT 상태의 발주서를 수정한다.
     */
    PurchaseOrderDetailResponse updateDraft(Long userId, Long purchaseOrderId, PurchaseOrderUpdateRequest request);

    /**
     * DRAFT 상태의 발주서를 제출한다.
     */
    PurchaseOrderDetailResponse submit(Long userId, Long purchaseOrderId);

    /**
     * SUBMITTED 상태의 발주서를 확정한다. (OWNER 전용)
     */
    PurchaseOrderDetailResponse confirm(Long userId, Long purchaseOrderId);

    /**
     * 발주서를 취소한다.
     */
    PurchaseOrderDetailResponse cancel(Long userId, Long purchaseOrderId);

    /**
     * 발주서 PDF를 생성하여 다운로드한다.
     */
    byte[] downloadPdf(Long userId, Long purchaseOrderId);
}
