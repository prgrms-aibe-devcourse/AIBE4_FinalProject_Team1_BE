package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
}
