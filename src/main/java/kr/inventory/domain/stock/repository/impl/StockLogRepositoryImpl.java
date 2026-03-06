package kr.inventory.domain.stock.repository.impl;

import static kr.inventory.domain.reference.entity.QIngredient.*;
import static kr.inventory.domain.stock.entity.QStockLog.*;
import static kr.inventory.domain.user.entity.QUser.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.stock.controller.dto.request.StockLogSearchCondition;
import kr.inventory.domain.stock.controller.dto.response.StockLogResponse;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.entity.enums.TransactionType;
import kr.inventory.domain.stock.repository.StockLogRepositoryCustom;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StockLogRepositoryImpl implements StockLogRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	@Override
	public Page<StockLogResponse> searchStockLog(
		Long storeId,
		StockLogSearchCondition condition,
		Pageable pageable
	) {
		BooleanExpression[] predicates = {
			storeIdEq(storeId),
			dateBetween(condition.startDate(), condition.endDate()),
			typeEq(condition.type()),
			ingredientNameContains(condition.ingredientName())
		};

		List<StockLog> content = queryFactory
			.selectFrom(stockLog)
			.join(stockLog.ingredient, ingredient).fetchJoin()
			.leftJoin(stockLog.createdByUser, user).fetchJoin() // 시스템 자동일 경우 null일 수 있으므로 leftJoin
			.where(predicates)
			.orderBy(stockLog.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(stockLog.count())
			.from(stockLog)
			// 카운트 시에는 ingredientName 검색이 없다면 fetchJoin이 필요 없지만,
			// ingredientName 검색 조건이 포함되어 있으므로 ingredient join은 필요함
			.leftJoin(stockLog.ingredient, ingredient)
			.where(predicates);

		return PageableExecutionUtils.getPage(
			content.stream().map(StockLogResponse::from).toList(),
			pageable,
			countQuery::fetchOne
		);
	}

	private BooleanExpression storeIdEq(Long storeId) {
		return storeId != null ? stockLog.store.storeId.eq(storeId) : null;
	}

	private BooleanExpression typeEq(TransactionType type) {
		return type != null ? stockLog.transactionType.eq(type) : null;
	}

	private BooleanExpression ingredientNameContains(String name) {
		return StringUtils.hasText(name) ? ingredient.name.contains(name) : null;
	}

	private BooleanExpression dateBetween(OffsetDateTime start, OffsetDateTime end) {
		if (start == null && end == null)
			return null;

		// 시작 시간만 있는 경우: 해당 시점 이후 (보통 시작일의 00:00:00으로 맞춰서 들어옴)
		if (start != null && end == null) {
			return stockLog.createdAt.goe(start);
		}

		// 종료 시간만 있는 경우: 해당 시점 이전 (보통 종료일의 23:59:59로 맞춰서 들어옴)
		if (start == null) {
			return stockLog.createdAt.loe(end);
		}

		// 둘 다 있는 경우: 사이값
		return stockLog.createdAt.between(start, end);
	}
}
