package kr.inventory.domain.sales.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

import static kr.inventory.domain.sales.entity.QSalesOrder.salesOrder;
import static kr.inventory.domain.sales.entity.QSalesOrderItem.salesOrderItem;
import static kr.inventory.domain.store.entity.QStore.store;

@RequiredArgsConstructor
public class SalesOrderRepositoryImpl implements SalesOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final SalesOrderItemRepository salesOrderItemRepository;

    @Override
    public Optional<SalesOrder> findByIdAndStoreStoreIdWithLock(Long salesOrderId, Long storeId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(salesOrder)
                        .innerJoin(salesOrder.store, store).fetchJoin()
                        .where(
                                salesOrder.salesOrderId.eq(salesOrderId),
                                salesOrder.store.storeId.eq(storeId)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public Optional<SalesOrder> findByOrderPublicIdWithItems(UUID orderPublicId, Long storeId) {
        SalesOrder order = queryFactory
                .selectFrom(salesOrder)
                .leftJoin(salesOrderItem).on(salesOrderItem.salesOrder.eq(salesOrder))
                .fetchJoin()
                .where(
                        salesOrder.orderPublicId.eq(orderPublicId),
                        salesOrder.store.storeId.eq(storeId)
                )
                .fetchOne();

        return Optional.ofNullable(order);
    }

    @Override
    public List<SalesOrderResponse> findStoreOrders(Long storeId) {
        List<SalesOrder> orders = queryFactory
                .selectFrom(salesOrder)
                .leftJoin(salesOrder.diningTable).fetchJoin()
                .where(salesOrder.store.storeId.eq(storeId))
                .orderBy(salesOrder.orderedAt.desc())
                .fetch();

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 주문 ID 추출
        List<Long> orderIds = orders.stream()
                .map(SalesOrder::getSalesOrderId)
                .toList();

        // 3. 모든 주문 항목을 한 번에 조회 (N+1 방지!)
        List<SalesOrderItem> allItems = salesOrderItemRepository.findBySalesOrderSalesOrderIdIn(orderIds);

        // 4. 주문 ID별로 항목 그룹핑 (메모리에서 매핑)
        Map<Long, List<SalesOrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getSalesOrder().getSalesOrderId()));

        // 5. Response 생성
        return orders.stream()
                .map(order -> {
                    List<SalesOrderItem> items = itemsByOrderId.getOrDefault(
                            order.getSalesOrderId(),
                            Collections.emptyList()
                    );
                    return SalesOrderResponse.from(order, items);
                })
                .collect(Collectors.toList());
    }
}