package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {
    List<SalesOrderItem> findBySalesOrderSalesOrderId(Long salesOrderId);
}
