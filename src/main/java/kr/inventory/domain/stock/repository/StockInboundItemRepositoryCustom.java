package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.repository.dto.InboundItemAggregate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockInboundItemRepositoryCustom {

    Optional<StockInboundItem> findWithInbound(UUID inboundItemPublicId);

    List<InboundItemAggregate> findAggregatesByInboundIds(List<Long> inboundIds);
}
