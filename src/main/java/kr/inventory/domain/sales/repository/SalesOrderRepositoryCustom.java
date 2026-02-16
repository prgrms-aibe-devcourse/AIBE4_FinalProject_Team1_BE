package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrder;

import java.util.Optional;

public interface SalesOrderRepositoryCustom {
    Optional<SalesOrder> findByIdAndStoreStoreIdWithLock(Long salesOrderId, Long storeId);
}
