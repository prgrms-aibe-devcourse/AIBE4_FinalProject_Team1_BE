package kr.inventory.domain.purchase.repository.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderSearchRequest;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static kr.inventory.domain.purchase.entity.QPurchaseOrder.purchaseOrder;
import static kr.inventory.domain.vendor.entity.QVendor.vendor;

@RequiredArgsConstructor
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PurchaseOrder> findByStoreIdWithFilters(Long storeId, PurchaseOrderSearchRequest searchRequest,
            Pageable pageable) {
        JPAQuery<PurchaseOrder> query = queryFactory
                .selectFrom(purchaseOrder)
                .leftJoin(purchaseOrder.vendor, vendor).fetchJoin()
                .where(
                        purchaseOrder.store.storeId.eq(storeId),
                        statusEq(searchRequest.status()),
                        searchContains(searchRequest.search()));

        // 정렬 적용
        for (OrderSpecifier<?> order : getOrderSpecifiers(pageable.getSort())) {
            query.orderBy(order);
        }

        List<PurchaseOrder> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(purchaseOrder.count())
                .from(purchaseOrder)
                .where(
                        purchaseOrder.store.storeId.eq(storeId),
                        statusEq(searchRequest.status()),
                        searchContains(searchRequest.search()))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression statusEq(PurchaseOrderStatus status) {
        return status != null ? purchaseOrder.status.eq(status) : null;
    }

    private BooleanExpression searchContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return purchaseOrder.orderNo.containsIgnoreCase(search)
                .or(purchaseOrder.vendor.name.containsIgnoreCase(search));
    }

    private List<OrderSpecifier<?>> getOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (!sort.isEmpty()) {
            for (Sort.Order order : sort) {
                com.querydsl.core.types.Order direction = order.isAscending()
                        ? com.querydsl.core.types.Order.ASC
                        : com.querydsl.core.types.Order.DESC;

                switch (order.getProperty()) {
                    case "orderNo" -> orders.add(new OrderSpecifier<>(direction, purchaseOrder.orderNo));
                    case "createdAt" -> orders.add(new OrderSpecifier<>(direction, purchaseOrder.createdAt));
                    default -> orders.add(new OrderSpecifier<>(direction, purchaseOrder.purchaseOrderId));
                }
            }
        } else {
            orders.add(purchaseOrder.purchaseOrderId.desc());
        }

        return orders;
    }
}
