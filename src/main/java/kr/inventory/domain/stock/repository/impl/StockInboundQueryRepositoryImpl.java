package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.reference.entity.QIngredient;
import kr.inventory.domain.reference.entity.QVendor;
import kr.inventory.domain.stock.entity.QStockInbound;
import kr.inventory.domain.stock.entity.QStockInboundItem;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.repository.StockInboundQueryRepository;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import kr.inventory.domain.store.entity.QStore;
import kr.inventory.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockInboundQueryRepositoryImpl implements StockInboundQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockInboundSummary> findInboundSummaries(
            UUID storePublicId,
            InboundStatus status,
            String keyword,
            int limit
    ) {
        QStockInbound stockInbound = QStockInbound.stockInbound;
        QStockInboundItem stockInboundItem = QStockInboundItem.stockInboundItem;
        QVendor vendor = QVendor.vendor;
        QIngredient ingredient = QIngredient.ingredient;
        QStore store = QStore.store;
        QUser confirmedByUser = new QUser("confirmedByUser");

        NumberExpression<Integer> normalizationNeededCount = new CaseBuilder()
                .when(needsNormalization(stockInboundItem))
                .then(1)
                .otherwise(0)
                .sum();

        Expression<Boolean> hasItemsNeedingNormalization = new CaseBuilder()
                .when(normalizationNeededCount.gt(0))
                .then(true)
                .otherwise(false);

        return queryFactory
                .select(Projections.constructor(
                        StockInboundSummary.class,
                        stockInbound.inboundPublicId,
                        stockInbound.inboundDate,
                        stockInbound.status,
                        vendor.name,
                        stockInboundItem.inboundItemId.countDistinct().intValue(),
                        confirmedByUser.name,
                        stockInbound.confirmedAt,
                        hasItemsNeedingNormalization
                ))
                .from(stockInbound)
                .join(stockInbound.store, store)
                .leftJoin(stockInbound.vendor, vendor)
                .leftJoin(stockInbound.confirmedByUser, confirmedByUser)
                .leftJoin(stockInboundItem).on(stockInboundItem.inbound.eq(stockInbound))
                .leftJoin(stockInboundItem.ingredient, ingredient)
                .where(
                        storePublicIdEq(storePublicId, store),
                        statusEq(status, stockInbound),
                        keywordContains(keyword, vendor, stockInboundItem, ingredient)
                )
                .groupBy(
                        stockInbound.inboundId,
                        stockInbound.inboundPublicId,
                        stockInbound.inboundDate,
                        stockInbound.status,
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

    private BooleanExpression storePublicIdEq(UUID storePublicId, QStore store) {
        return store.storePublicId.eq(storePublicId);
    }

    private BooleanExpression statusEq(InboundStatus status, QStockInbound stockInbound) {
        return status == null ? null : stockInbound.status.eq(status);
    }

    private BooleanExpression keywordContains(
            String keyword,
            QVendor vendor,
            QStockInboundItem stockInboundItem,
            QIngredient ingredient
    ) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return vendor.name.containsIgnoreCase(keyword)
                .or(stockInboundItem.rawProductName.containsIgnoreCase(keyword))
                .or(stockInboundItem.productDisplayName.containsIgnoreCase(keyword))
                .or(ingredient.name.containsIgnoreCase(keyword));
    }

    private BooleanExpression needsNormalization(QStockInboundItem stockInboundItem) {
        return stockInboundItem.ingredient.isNull()
                .or(stockInboundItem.resolutionStatus.isNull())
                .or(stockInboundItem.resolutionStatus.ne(ResolutionStatus.CONFIRMED));
    }
}