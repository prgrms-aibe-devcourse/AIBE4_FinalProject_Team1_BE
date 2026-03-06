package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.repository.StockInboundItemRepositoryCustom;
import kr.inventory.domain.stock.repository.dto.InboundItemAggregate;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.inventory.domain.stock.entity.QStockInboundItem.stockInboundItem;
import static kr.inventory.domain.stock.entity.QStockInbound.stockInbound;
import static kr.inventory.domain.store.entity.QStore.store;

@RequiredArgsConstructor
public class StockInboundItemRepositoryImpl implements StockInboundItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<StockInboundItem> findWithInbound(UUID inboundItemPublicId) {
        StockInboundItem result = queryFactory
                .selectFrom(stockInboundItem)
                .join(stockInboundItem.inbound, stockInbound).fetchJoin()
                .join(stockInbound.store, store).fetchJoin()
                .where(stockInboundItem.inboundItemPublicId.eq(inboundItemPublicId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<InboundItemAggregate> findAggregatesByInboundIds(List<Long> inboundIds) {
        if (inboundIds == null || inboundIds.isEmpty()) {
            return Collections.emptyList();
        }

        // resolutionStatus가 null 이거나 CONFIRMED가 아니면 미해결로 간주
        NumberExpression<Long> unresolvedOne = new CaseBuilder()
                .when(stockInboundItem.resolutionStatus.isNull()
                        .or(stockInboundItem.resolutionStatus.ne(ResolutionStatus.CONFIRMED)))
                .then(1L)
                .otherwise(0L);

        // unitCost가 null일 수 있으므로 coalesce
        NumberExpression<BigDecimal> lineCost = stockInboundItem.unitCost.coalesce(BigDecimal.ZERO)
                .multiply(stockInboundItem.quantity.coalesce(BigDecimal.ZERO));

        return queryFactory
                .select(Projections.constructor(
                        InboundItemAggregate.class,
                        stockInboundItem.inbound.inboundId,
                        stockInboundItem.inboundItemId.count(),
                        unresolvedOne.sum(),
                        lineCost.sum()
                ))
                .from(stockInboundItem)
                .where(stockInboundItem.inbound.inboundId.in(inboundIds))
                .groupBy(stockInboundItem.inbound.inboundId)
                .fetch();
    }
}
