package kr.inventory.domain.vendor.repository.impl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.vendor.entity.QVendor;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kr.inventory.domain.vendor.entity.QVendor.vendor;

@RequiredArgsConstructor
public class VendorRepositoryImpl implements VendorRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<Vendor> findByStoreIdWithFilters(Long storeId, VendorStatus status, String search, Pageable pageable) {
		// 데이터 조회
		JPAQuery<Vendor> query = queryFactory
			.selectFrom(vendor)
			.where(
				vendor.store.storeId.eq(storeId),
				statusEq(status),
				searchContains(search)
			);

		// 정렬 적용
		for (OrderSpecifier<?> order : getOrderSpecifiers(pageable.getSort())) {
			query.orderBy(order);
		}

		// 페이징 적용
		List<Vendor> content = query
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		// 전체 개수 조회
		Long total = queryFactory
			.select(vendor.count())
			.from(vendor)
			.where(
				vendor.store.storeId.eq(storeId),
				statusEq(status),
				searchContains(search)
			)
			.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total : 0L);
	}

	private BooleanExpression statusEq(VendorStatus status) {
		return status != null ? vendor.status.eq(status) : null;
	}

	private BooleanExpression searchContains(String search) {
		return search != null && !search.isBlank()
			? vendor.name.containsIgnoreCase(search)
			: null;
	}

	private List<OrderSpecifier<?>> getOrderSpecifiers(Sort sort) {
		List<OrderSpecifier<?>> orders = new ArrayList<>();

		if (!sort.isEmpty()) {
			for (Sort.Order order : sort) {
				com.querydsl.core.types.Order direction = order.isAscending()
					? com.querydsl.core.types.Order.ASC
					: com.querydsl.core.types.Order.DESC;

				switch (order.getProperty()) {
					case "name" -> orders.add(new OrderSpecifier<>(direction, vendor.name));
					case "createdAt" -> orders.add(new OrderSpecifier<>(direction, vendor.createdAt));
					default -> orders.add(new OrderSpecifier<>(direction, vendor.createdAt));
				}
			}
		} else {
			// 기본 정렬: 생성일 내림차순
			orders.add(vendor.createdAt.desc());
		}

		return orders;
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
