package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockInboundItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockInboundItemRepository extends JpaRepository<StockInboundItem, Long>, StockInboundItemRepositoryCustom {

	List<StockInboundItem> findByInboundInboundId(Long inboundId);

    @EntityGraph(attributePaths = {"ingredient"})
    List<StockInboundItem> findByInbound_InboundIdOrderByInboundItemIdAsc(Long inboundId);

}
