package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.stock.controller.dto.request.StockShortageSearchRequest;
import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import kr.inventory.domain.stock.repository.StockShortageRepositoryCustom;
import kr.inventory.domain.stock.service.command.ShortageRelatedOrderQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static kr.inventory.domain.reference.entity.QIngredient.ingredient;
import static kr.inventory.domain.sales.entity.QSalesOrder.salesOrder;
import static kr.inventory.domain.stock.entity.QStockShortage.stockShortage;

@RequiredArgsConstructor
public class StockShortageRepositoryImpl implements StockShortageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Long> findDistinctSalesOrderIdsByStoreId(
            Long storeId,
            StockShortageSearchRequest searchRequest,
            Pageable pageable
    ) {
        List<Long> content = queryFactory
                .select(stockShortage.salesOrderId)
                .from(stockShortage)
                .where(
                        stockShortage.storeId.eq(storeId),
                        createdAtGoe(searchRequest.from()),
                        createdAtLoe(searchRequest.to())
                )
                .groupBy(stockShortage.salesOrderId)
                .orderBy(stockShortage.createdAt.max().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(stockShortage.salesOrderId.countDistinct())
                .from(stockShortage)
                .where(
                        stockShortage.storeId.eq(storeId),
                        createdAtGoe(searchRequest.from()),
                        createdAtLoe(searchRequest.to())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<StockShortage> findAllBySalesOrderIds(
            List<Long> salesOrderIds,
            StockShortageSearchRequest searchRequest
    ) {
        return queryFactory
                .selectFrom(stockShortage)
                .where(
                        stockShortage.salesOrderId.in(salesOrderIds),
                        createdAtGoe(searchRequest.from()),
                        createdAtLoe(searchRequest.to())
                )
                .fetch();
    }

    private BooleanExpression createdAtGoe(OffsetDateTime from) {
        return from != null ? stockShortage.createdAt.goe(from) : null;
    }

    private BooleanExpression createdAtLoe(OffsetDateTime to) {
        return to != null ? stockShortage.createdAt.loe(to) : null;
    }

    @Override
    public Set<Long> findPendingIngredientIds(Long storeId, List<Long> ingredientIds) {

        List<Long> result = queryFactory
                .select(stockShortage.ingredientId)
                .distinct()
                .from(stockShortage)
                .where(
                        stockShortage.storeId.eq(storeId),
                        stockShortage.ingredientId.in(ingredientIds),
                        stockShortage.status.eq(ShortageStatus.PENDING)
                )
                .fetch();

        return new HashSet<>(result);
    }

    @Override
    public List<StockShortage> findPendingShortages(Long storeId, Collection<Long> ingredientIds, ShortageStatus status) {
        return queryFactory
                .selectFrom(stockShortage)
                .where(
                        stockShortage.storeId.eq(storeId),
                        stockShortage.ingredientId.in(ingredientIds),
                        stockShortage.status.eq(status)
                )
                .orderBy(stockShortage.ingredientId.asc(), stockShortage.createdAt.asc())
                .fetch();
    }

    @Override
    public Optional<ShortageRelatedOrderQueryResult> findShortageRelatedOrder(
            Long storeId,
            Long stockShortageId
    ) {
        ShortageRelatedOrderQueryResult result = queryFactory
                .select(Projections.constructor(
                        ShortageRelatedOrderQueryResult.class,
                        stockShortage.stockShortagePublicId,
                        stockShortage.status.stringValue(),
                        stockShortage.createdAt,
                        stockShortage.closedAt,
                        ingredient.ingredientPublicId,
                        ingredient.name,
                        stockShortage.requiredAmount,
                        stockShortage.shortageAmount,
                        salesOrder.orderPublicId,
                        salesOrder.orderedAt,
                        salesOrder.completedAt,
                        salesOrder.status.stringValue(),
                        salesOrder.type.stringValue(),
                        salesOrder.totalAmount
                ))
                .from(stockShortage)
                .join(ingredient).on(
                        ingredient.ingredientId.eq(stockShortage.ingredientId),
                        ingredient.store.storeId.eq(storeId)
                )
                .join(salesOrder).on(
                        salesOrder.salesOrderId.eq(stockShortage.salesOrderId),
                        salesOrder.store.storeId.eq(storeId)
                )
                .where(
                        stockShortage.stockShortageId.eq(stockShortageId),
                        stockShortage.storeId.eq(storeId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}