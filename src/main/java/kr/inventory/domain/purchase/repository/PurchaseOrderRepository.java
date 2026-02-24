package kr.inventory.domain.purchase.repository;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
}
