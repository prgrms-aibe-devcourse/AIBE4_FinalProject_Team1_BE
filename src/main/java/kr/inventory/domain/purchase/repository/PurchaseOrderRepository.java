package kr.inventory.domain.purchase.repository;

import kr.inventory.domain.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByStoreStoreIdOrderByPurchaseOrderIdDesc(Long storeId);

    Optional<PurchaseOrder> findByPurchaseOrderPublicId(UUID purchaseOrderPublicId);
}
