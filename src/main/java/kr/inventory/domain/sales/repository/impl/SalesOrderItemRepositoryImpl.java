package kr.inventory.domain.sales.repository.impl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.sales.repository.SalesOrderItemRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kr.inventory.domain.sales.entity.QSalesOrderItem.salesOrderItem;

@RequiredArgsConstructor
public class SalesOrderItemRepositoryImpl implements SalesOrderItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, Long> countItemsBySalesOrderIds(List<Long> salesOrderIds) {
        if (salesOrderIds == null || salesOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Tuple> tuples = queryFactory
                .select(salesOrderItem.salesOrder.salesOrderId, salesOrderItem.salesOrderItemId.count())
                .from(salesOrderItem)
                .where(salesOrderItem.salesOrder.salesOrderId.in(salesOrderIds))
                .groupBy(salesOrderItem.salesOrder.salesOrderId)
                .fetch();

        return tuples.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(salesOrderItem.salesOrder.salesOrderId),
                        tuple -> tuple.get(salesOrderItem.salesOrderItemId.count())
                ));
    }
}
