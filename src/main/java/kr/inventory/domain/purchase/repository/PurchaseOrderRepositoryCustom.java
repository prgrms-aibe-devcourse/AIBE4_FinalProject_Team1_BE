package kr.inventory.domain.purchase.repository;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderSearchRequest;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderRepositoryCustom {
    Page<PurchaseOrder> findByStoreIdWithFilters(Long storeId, PurchaseOrderSearchRequest searchRequest,
            Pageable pageable);
}
