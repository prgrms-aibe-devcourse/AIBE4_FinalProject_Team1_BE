package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, SalesOrderRepositoryCustom {

    Optional<SalesOrder> findByStoreStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);

    Optional<SalesOrder> findByOrderPublicIdAndStoreStoreId(UUID orderPublicId, Long storeId);
}
