package kr.inventory.domain.sales.repository.impl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.dining.entity.QDiningTable;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerTotalSummaryResponse;
import kr.inventory.domain.sales.entity.QSalesOrderItem;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.repository.SalesOrderRepositoryCustom;
import kr.inventory.domain.sales.service.command.SalesLedgerQueryCondition;
import kr.inventory.domain.sales.service.command.SalesLedgerSortBy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
        QDiningTable diningTable = QDiningTable.diningTable;

        SalesOrder order = queryFactory
                .selectFrom(salesOrder)
                .leftJoin(salesOrder.diningTable, diningTable).fetchJoin()
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
        return findSalesLedgerOrders(
                storeId,
                new SalesLedgerQueryCondition(
                        from,
                        to,
                        status,
                        type,
                        null,
                        null,
                        null,
                        null,
                        SalesLedgerSortBy.ORDERED_AT_DESC
                ),
                pageable
        );
    }

    @Override
    public Page<SalesOrder> findSalesLedgerOrders(Long storeId, SalesLedgerQueryCondition condition, Pageable pageable) {
        QDiningTable diningTable = QDiningTable.diningTable;
        BooleanExpression[] conditions = ledgerConditions(storeId, condition);

        List<SalesOrder> orders = queryFactory
                .selectFrom(salesOrder)
                .leftJoin(salesOrder.diningTable, diningTable).fetchJoin()
                .where(conditions)
                .orderBy(orderSpecifiers(condition.sortBy()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(salesOrder.count())
                .from(salesOrder)
                .leftJoin(salesOrder.diningTable, diningTable)
                .where(conditions)
                .fetchOne();

        long safeTotalCount = totalCount == null ? 0L : totalCount;
        return new PageImpl<>(orders, pageable, safeTotalCount);
    }

    @Override
    public SalesLedgerTotalSummaryResponse calculateSalesLedgerSummary(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            SalesOrderStatus status,
            SalesOrderType type
    ) {
        return calculateSalesLedgerSummary(
                storeId,
                new SalesLedgerQueryCondition(
                        from,
                        to,
                        status,
                        type,
                        null,
                        null,
                        null,
                        null,
                        SalesLedgerSortBy.ORDERED_AT_DESC
                )
        );
    }

    @Override
    public SalesLedgerTotalSummaryResponse calculateSalesLedgerSummary(Long storeId, SalesLedgerQueryCondition condition) {
        QDiningTable diningTable = QDiningTable.diningTable;
        BooleanExpression[] conditions = ledgerConditions(storeId, condition);

        Tuple results = queryFactory
                .select(
                        salesOrder.count(),
                        salesOrder.totalAmount.sum(),
                        salesOrder.status.when(SalesOrderStatus.REFUNDED).then(salesOrder.totalAmount).otherwise(BigDecimal.ZERO).sum()
                )
                .from(salesOrder)
                .leftJoin(salesOrder.diningTable, diningTable)
                .where(conditions)
                .fetchOne();

        if (results == null) {
            return new SalesLedgerTotalSummaryResponse(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long count = results.get(0, Long.class) != null ? results.get(0, Long.class) : 0L;
        BigDecimal totalAmount = results.get(1, BigDecimal.class) != null ? results.get(1, BigDecimal.class) : BigDecimal.ZERO;
        BigDecimal totalRefundAmount = results.get(2, BigDecimal.class) != null ? results.get(2, BigDecimal.class) : BigDecimal.ZERO;
        BigDecimal totalNetAmount = totalAmount.subtract(totalRefundAmount);

        return new SalesLedgerTotalSummaryResponse(count, totalAmount, totalRefundAmount, totalNetAmount);
    }

    private BooleanExpression[] ledgerConditions(Long storeId, SalesLedgerQueryCondition condition) {
        List<BooleanExpression> conditions = new ArrayList<>();
        conditions.add(salesOrder.store.storeId.eq(storeId));
        conditions.add(salesOrder.orderedAt.goe(condition.from().withOffsetSameInstant(ZoneOffset.UTC)));
        conditions.add(salesOrder.orderedAt.loe(condition.to().withOffsetSameInstant(ZoneOffset.UTC)));
        conditions.add(salesOrderTypeEq(condition.type()));
        conditions.add(salesOrderStatusEq(condition.status()));
        conditions.add(menuNameContains(condition.menuName()));
        conditions.add(totalAmountGoe(condition.amountMin()));
        conditions.add(totalAmountLoe(condition.amountMax()));
        conditions.add(tableCodeContains(condition.tableCode()));
        return conditions.stream().filter(java.util.Objects::nonNull).toArray(BooleanExpression[]::new);
    }

    private BooleanExpression salesOrderTypeEq(SalesOrderType type) {
        return type == null ? null : salesOrder.type.eq(type);
    }

    private BooleanExpression salesOrderStatusEq(SalesOrderStatus status) {
        return status == null ? null : salesOrder.status.eq(status);
    }

    private BooleanExpression menuNameContains(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            return null;
        }

        QSalesOrderItem itemFilter = new QSalesOrderItem("itemFilter");
        return JPAExpressions
                .selectOne()
                .from(itemFilter)
                .where(
                        itemFilter.salesOrder.eq(salesOrder),
                        itemFilter.menuName.containsIgnoreCase(menuName)
                )
                .exists();
    }

    private BooleanExpression totalAmountGoe(BigDecimal amountMin) {
        return amountMin == null ? null : salesOrder.totalAmount.goe(amountMin);
    }

    private BooleanExpression totalAmountLoe(BigDecimal amountMax) {
        return amountMax == null ? null : salesOrder.totalAmount.loe(amountMax);
    }

    private BooleanExpression tableCodeContains(String tableCode) {
        if (tableCode == null || tableCode.isBlank()) {
            return null;
        }
        return salesOrder.diningTable.tableCode.containsIgnoreCase(tableCode);
    }

    private OrderSpecifier<?>[] orderSpecifiers(SalesLedgerSortBy sortBy) {
        SalesLedgerSortBy safeSortBy = sortBy != null ? sortBy : SalesLedgerSortBy.ORDERED_AT_DESC;
        return switch (safeSortBy) {
            case ORDERED_AT_ASC -> new OrderSpecifier[]{salesOrder.orderedAt.asc(), salesOrder.salesOrderId.asc()};
            case TOTAL_AMOUNT_DESC -> new OrderSpecifier[]{salesOrder.totalAmount.desc(), salesOrder.orderedAt.desc(), salesOrder.salesOrderId.desc()};
            case TOTAL_AMOUNT_ASC -> new OrderSpecifier[]{salesOrder.totalAmount.asc(), salesOrder.orderedAt.asc(), salesOrder.salesOrderId.asc()};
            case ORDERED_AT_DESC -> new OrderSpecifier[]{salesOrder.orderedAt.desc(), salesOrder.salesOrderId.desc()};
        };
    }
}
