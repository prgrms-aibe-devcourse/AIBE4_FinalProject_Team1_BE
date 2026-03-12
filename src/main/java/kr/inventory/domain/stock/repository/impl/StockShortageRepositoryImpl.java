package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.stock.controller.dto.request.StockShortageSearchRequest;
import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import kr.inventory.domain.stock.repository.StockShortageRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
}