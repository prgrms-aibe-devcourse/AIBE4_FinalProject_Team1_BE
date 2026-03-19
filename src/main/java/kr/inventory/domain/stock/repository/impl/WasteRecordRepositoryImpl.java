package kr.inventory.domain.stock.repository.impl;

import static kr.inventory.domain.reference.entity.QIngredient.*;
import static kr.inventory.domain.stock.entity.QWasteRecord.*;
import static kr.inventory.domain.user.entity.QUser.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.stock.controller.dto.request.WasteSearchRequest;
import kr.inventory.domain.stock.entity.WasteRecord;
import kr.inventory.domain.stock.entity.enums.WasteReason;
import kr.inventory.domain.stock.repository.WasteRecordRepositoryCustom;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WasteRecordRepositoryImpl implements WasteRecordRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<WasteRecord> searchWasteRecords(Long storeId, WasteSearchRequest condition, Pageable pageable) {

		// 1. 데이터 조회 쿼리
		List<WasteRecord> content = queryFactory
			.selectFrom(wasteRecord)
			.join(wasteRecord.ingredient, ingredient).fetchJoin() // 식재료 정보 포함
			.join(wasteRecord.recordedByUser, user).fetchJoin()    // 담당자 정보 포함
			.where(
				wasteRecord.store.storeId.eq(storeId),
				betweenDate(condition.startAt(), condition.endAt()),
				reasonEq(condition.reason()),
				ingredientNameLike(condition.ingredientName())
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(getAllOrderSpecifiers(pageable))
			.fetch();

		// 2. 카운트 쿼리 (최적화를 위해 별도 실행)
		JPAQuery<Long> countQuery = queryFactory
			.select(wasteRecord.count())
			.from(wasteRecord)
			.where(
				wasteRecord.store.storeId.eq(storeId),
				betweenDate(condition.startAt(), condition.endAt()),
				reasonEq(condition.reason()),
				ingredientNameLike(condition.ingredientName())
			);

		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	private OrderSpecifier<?>[] getAllOrderSpecifiers(Pageable pageable) {
		List<OrderSpecifier<?>> orders = new ArrayList<>();

		if (!pageable.getSort().isEmpty()) {
			for (Sort.Order sortOrder : pageable.getSort()) {
				Order direction = sortOrder.getDirection().isAscending() ? Order.ASC : Order.DESC;

				switch (sortOrder.getProperty()) {
					case "wasteDate":
						orders.add(new OrderSpecifier<>(direction, wasteRecord.wasteDate));
						break;
					case "amount":
						orders.add(new OrderSpecifier<>(direction, wasteRecord.wasteAmount));
						break;
					default:
						orders.add(new OrderSpecifier<>(Order.DESC, wasteRecord.wasteDate));
						break;
				}
			}
		} else {
			orders.add(new OrderSpecifier<>(Order.DESC, wasteRecord.wasteDate));
		}

		return orders.toArray(new OrderSpecifier[0]);
	}

	private BooleanExpression betweenDate(OffsetDateTime start, OffsetDateTime end) {
		if (start == null && end == null)
			return null;
		if (start == null)
			return wasteRecord.wasteDate.loe(end);
		if (end == null)
			return wasteRecord.wasteDate.goe(start);
		return wasteRecord.wasteDate.between(start, end);
	}

	private BooleanExpression reasonEq(WasteReason reason) {
		return reason != null ? wasteRecord.wasteReason.eq(reason) : null;
	}

	private BooleanExpression ingredientNameLike(String name) {
		return StringUtils.hasText(name) ? wasteRecord.ingredient.name.contains(name) : null;
	}
}