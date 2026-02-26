package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockInboundItemRepository extends JpaRepository<StockInboundItem, Long> {
	List<StockInboundItem> findByInbound_InboundId(Long inboundId);
    void deleteAllByInbound_InboundId(Long inboundId);
}
