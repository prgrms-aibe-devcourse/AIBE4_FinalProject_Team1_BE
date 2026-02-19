package kr.inventory.domain.sales.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.sales.entity.SalesOrder;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static kr.inventory.domain.sales.entity.QSalesOrder.salesOrder;
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
}