package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockInboundItemRepository extends JpaRepository<StockInboundItem, Long>, StockInboundItemRepositoryCustom {

	List<StockInboundItem> findByInboundInboundId(Long inboundId);

}
