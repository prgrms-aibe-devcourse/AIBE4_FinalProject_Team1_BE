package kr.inventory.domain.sales.repository.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.repository.SalesOrderRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.inventory.domain.sales.entity.QSalesOrder.salesOrder;
import static kr.inventory.domain.sales.entity.QSalesOrderItem.salesOrderItem;
import static kr.inventory.domain.store.entity.QStore.store;

@RequiredArgsConstructor
public class SalesOrderRepositoryImpl implements SalesOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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
    public Page<SalesOrder> findStoreOrders(Long storeId, Pageable pageable) {
        List<SalesOrder> orders = queryFactory
                .selectFrom(salesOrder)
                .leftJoin(salesOrder.diningTable).fetchJoin()
                .where(salesOrder.store.storeId.eq(storeId))
                .orderBy(salesOrder.orderedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(salesOrder.count())
                .from(salesOrder)
                .where(salesOrder.store.storeId.eq(storeId))
                .fetchOne();

        long safeTotalCount = totalCount == null ? 0L : totalCount;
        return new PageImpl<>(orders, pageable, safeTotalCount);
    }

    @Override
    public Page<SalesOrder> findSalesLedgerOrders(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            SalesOrderStatus status,
            SalesOrderType type,
            Pageable pageable
    ) {
        BooleanExpression[] conditions = {
                salesOrder.store.storeId.eq(storeId),
                salesOrder.orderedAt.goe(from),
                salesOrder.orderedAt.loe(to),
                salesOrderTypeEq(type),
                salesOrderStatusEq(status)
        };

        List<SalesOrder> orders = queryFactory
                .selectFrom(salesOrder)
                .leftJoin(salesOrder.diningTable).fetchJoin()
                .where(conditions)
                .orderBy(salesOrder.orderedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(salesOrder.count())
                .from(salesOrder)
                .where(conditions)
                .fetchOne();

        long safeTotalCount = totalCount == null ? 0L : totalCount;
        return new PageImpl<>(orders, pageable, safeTotalCount);
    }

    private BooleanExpression salesOrderTypeEq(SalesOrderType type) {
        if (type == null) {
            return null;
        }
        return salesOrder.type.eq(type);
    }

    private BooleanExpression salesOrderStatusEq(SalesOrderStatus status) {
        if (status == null) {
            return null;
        }
        return salesOrder.status.eq(status);
    }
}