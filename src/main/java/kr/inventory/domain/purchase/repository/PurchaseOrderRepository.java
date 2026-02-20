package kr.inventory.domain.purchase.repository;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByStoreStoreIdOrderByPurchaseOrderIdDesc(Long storeId);

    @EntityGraph(attributePaths = {"items", "store"})
    Optional<PurchaseOrder> findWithItemsByPurchaseOrderId(Long purchaseOrderId);
}
