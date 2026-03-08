package kr.inventory.domain.stock.repository.impl;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.stock.repository.StockShortageRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static kr.inventory.domain.stock.entity.QStockShortage.stockShortage;

@RequiredArgsConstructor
public class StockShortageRepositoryImpl implements StockShortageRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Long> findDistinctSalesOrderIdsByStoreId(Long storeId, Pageable pageable) {
        List<Long> content = queryFactory
                .select(stockShortage.salesOrderId)
                .from(stockShortage)
                .where(stockShortage.storeId.eq(storeId))
                .groupBy(stockShortage.salesOrderId)
                .orderBy(stockShortage.createdAt.max().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(stockShortage.salesOrderId.countDistinct())
                .from(stockShortage)
                .where(stockShortage.storeId.eq(storeId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
