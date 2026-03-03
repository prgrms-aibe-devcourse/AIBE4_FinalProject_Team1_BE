package kr.inventory.domain.purchase.repository;

import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    List<PurchaseOrderItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);
}