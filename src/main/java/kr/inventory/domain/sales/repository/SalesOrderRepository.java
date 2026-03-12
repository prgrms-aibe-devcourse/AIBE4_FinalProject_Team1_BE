package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, SalesOrderRepositoryCustom {

    Optional<SalesOrder> findByStoreStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);

    Optional<SalesOrder> findByOrderPublicIdAndStoreStoreId(UUID orderPublicId, Long storeId);

    Page<SalesOrder> findByStatus(SalesOrderStatus status, Pageable pageable);

    long countByStatus(SalesOrderStatus status);
}
