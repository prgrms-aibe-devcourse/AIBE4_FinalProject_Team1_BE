package kr.inventory.domain.stock.repository.impl;

import static kr.inventory.domain.reference.entity.QIngredient.*;
import static kr.inventory.domain.stock.entity.QStockLog.*;
import static kr.inventory.domain.user.entity.QUser.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.stock.controller.dto.request.StockLogSearchRequest;
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
		StockLogSearchRequest condition,
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
			.leftJoin(stockLog.createdByUser, user).fetchJoin()
			.where(predicates)
			.orderBy(stockLog.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(stockLog.count())
			.from(stockLog)
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
		if (start != null && end == null) {
			return stockLog.createdAt.goe(start);
		}
		if (start == null) {
			return stockLog.createdAt.loe(end);
		}
		return stockLog.createdAt.between(start, end);
	}
}
