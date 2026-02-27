package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepositoryCustom {
    Optional<SalesOrder> findByIdAndStoreStoreIdWithLock(Long salesOrderId, Long storeId);

    Optional<SalesOrder> findByOrderPublicIdWithItems(UUID orderPublicId, Long storeId);

    List<SalesOrderResponse> findStoreOrders(Long storeId);
}
