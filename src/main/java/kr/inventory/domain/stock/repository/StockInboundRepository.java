package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockInbound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockInboundRepository extends JpaRepository<StockInbound, Long> {
    Page<StockInbound> findByStoreStoreId(Long storeId, Pageable pageable);
}
