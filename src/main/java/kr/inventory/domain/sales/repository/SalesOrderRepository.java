package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    Optional<SalesOrder> findByIdAndStoreStoreId(Long salesOrderId, Long storeId);
}
