package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockInboundRepository extends JpaRepository<StockInbound, Long>, StockInboundRepositoryCustom {
    Page<StockInbound> findByStoreStoreIdAndStatus(Long storeId, InboundStatus status, Pageable pageable);

    Page<StockInbound> findByStoreStoreIdAndStatusIn(Long storeId, List<InboundStatus> statuses, Pageable pageable);

    Optional<StockInbound> findByInboundPublicIdAndStoreStoreId(UUID inboundPublicId, Long storeId);

    Optional<StockInbound> findByInboundPublicIdAndStore_StoreId(UUID inboundPublicId, Long storeId);

    Page<StockInbound> findByStatus(InboundStatus status, Pageable pageable);

    long countByStatus(InboundStatus status);
}
