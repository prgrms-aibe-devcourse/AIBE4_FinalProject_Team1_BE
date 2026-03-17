package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.reference.entity.QVendor;
import kr.inventory.domain.stock.entity.QStockInbound;
import kr.inventory.domain.stock.entity.QStockInboundItem;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.repository.StockInboundQueryRepository;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import kr.inventory.domain.store.entity.QStore;
import kr.inventory.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockInboundQueryRepositoryImpl implements StockInboundQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockInboundSummary> findInboundSummaries(
            Long storeId,
            String keyword,
            int limit
    ) {
        QStockInbound stockInbound = QStockInbound.stockInbound;
        QStockInboundItem stockInboundItem = QStockInboundItem.stockInboundItem;
        QVendor vendor = QVendor.vendor;
        QStore store = QStore.store;
        QUser confirmedByUser = new QUser("confirmedByUser");

        return queryFactory
                .select(Projections.constructor(
                        StockInboundSummary.class,
                        stockInbound.inboundPublicId,
                        stockInbound.inboundDate,
                        vendor.name,
                        stockInboundItem.inboundItemId.countDistinct().intValue(),
                        confirmedByUser.name,
                        stockInbound.confirmedAt
                ))
                .from(stockInbound)
                .join(stockInbound.store, store)
                .leftJoin(stockInbound.vendor, vendor)
                .leftJoin(stockInbound.confirmedByUser, confirmedByUser)
                .leftJoin(stockInboundItem).on(stockInboundItem.inbound.eq(stockInbound))
                .where(
                        storeIdEq(storeId, store),
                        confirmedOnly(stockInbound),
                        keywordContains(keyword, vendor, stockInboundItem)
                )
                .groupBy(
                        stockInbound.inboundId,
                        stockInbound.inboundPublicId,
                        stockInbound.inboundDate,
                        vendor.name,
                        confirmedByUser.name,
                        stockInbound.confirmedAt
                )
                .orderBy(
                        stockInbound.inboundDate.desc(),
                        stockInbound.inboundId.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression storeIdEq(Long storeId, QStore store) {
        return store.storeId.eq(storeId);
    }

    private BooleanExpression confirmedOnly(QStockInbound stockInbound) {
        return stockInbound.status.eq(InboundStatus.CONFIRMED);
    }

    private BooleanExpression keywordContains(
            String keyword,
            QVendor vendor,
            QStockInboundItem stockInboundItem
    ) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return vendor.name.containsIgnoreCase(keyword)
                .or(stockInboundItem.rawProductName.containsIgnoreCase(keyword))
                .or(stockInboundItem.productDisplayName.containsIgnoreCase(keyword));
    }
}