package kr.inventory.domain.vendor.repository.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.vendor.entity.QVendor;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

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

	public Optional<Vendor> findMostSimilarVendor(Long storeId, String rawName) {
		QVendor vendor = QVendor.vendor;

		NumberExpression<Double> word_similarity = Expressions.numberTemplate(Double.class,
			"function('word_similarity', {0}, {1})", vendor.name, rawName);

		Vendor result = queryFactory
			.selectFrom(vendor)
			.where(
				vendor.store.storeId.eq(storeId),
				word_similarity.gt(0.3) // 최소 임계치 설정 (0.3 미만은 무시)
			)
			.orderBy(word_similarity.desc()) // 유사도가 높은 순서대로
			.fetchFirst();

		return Optional.ofNullable(result);
	}
}
