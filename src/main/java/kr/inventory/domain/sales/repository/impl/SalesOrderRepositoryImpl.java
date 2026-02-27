package kr.inventory.domain.sales.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
                .leftJoin(salesOrderItem).on(salesOrderItem.salesOrder.eq(salesOrder))
                .fetchJoin()
                .leftJoin(salesOrder.diningTable).fetchJoin()
                .where(salesOrder.store.storeId.eq(storeId))
                .orderBy(salesOrder.orderedAt.desc())
                .fetch();

        return orders.stream()
                .map(order -> {
                    List<SalesOrderItem> items = salesOrderItemRepository
                            .findBySalesOrderSalesOrderId(order.getSalesOrderId());
                    return SalesOrderResponse.from(order, items);
                })
                .collect(Collectors.toList());
    }
}