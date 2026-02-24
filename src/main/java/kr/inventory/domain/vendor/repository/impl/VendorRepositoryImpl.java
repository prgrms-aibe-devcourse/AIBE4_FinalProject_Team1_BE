package kr.inventory.domain.vendor.repository.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static kr.inventory.domain.vendor.entity.QVendor.vendor;

@RequiredArgsConstructor
public class VendorRepositoryImpl implements VendorRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Vendor> findByStoreIdWithFilters(Long storeId, VendorStatus status) {
        return queryFactory
                .selectFrom(vendor)
                .where(
                        vendor.store.storeId.eq(storeId),
                        statusEq(status)
                )
                .orderBy(vendor.createdAt.desc())
                .fetch();
    }

    private BooleanExpression statusEq(VendorStatus status) {
        return status != null ? vendor.status.eq(status) : null;
    }
}
