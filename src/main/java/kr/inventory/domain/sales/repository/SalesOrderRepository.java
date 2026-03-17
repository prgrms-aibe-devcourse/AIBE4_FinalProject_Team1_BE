package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, SalesOrderRepositoryCustom {

    Optional<SalesOrder> findByStoreStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);

    Optional<SalesOrder> findByOrderPublicIdAndStoreStoreId(UUID orderPublicId, Long storeId);

    // COMPLETED + REFUNDED 모두 조회 (BulkIndexingRunner용)
    Page<SalesOrder> findByStatusIn(List<SalesOrderStatus> statuses, Pageable pageable);
    long countByStatusIn(List<SalesOrderStatus> statuses);
}
