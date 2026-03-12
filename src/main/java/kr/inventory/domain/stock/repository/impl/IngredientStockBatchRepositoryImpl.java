package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.controller.dto.request.StockSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockSummaryResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.QIngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchSourceType;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepositoryCustom;
import kr.inventory.domain.stock.service.command.IngredientStockTotal;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static kr.inventory.domain.stock.entity.QIngredientStockBatch.ingredientStockBatch;

@RequiredArgsConstructor
public class IngredientStockBatchRepositoryImpl implements IngredientStockBatchRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<IngredientStockBatch> findAvailableBatchesByStoreWithLock(Long storeId,
		Collection<Long> ingredientIds) {
		if (storeId == null || ingredientIds == null || ingredientIds.isEmpty()) {
			return Collections.emptyList();
		}

		NumberExpression<Integer> sourcePriority = new CaseBuilder()
			.when(
				ingredientStockBatch.sourceType.eq(StockBatchSourceType.INBOUND)
					.and(ingredientStockBatch.expirationDate.isNotNull())
			).then(0)
			.when(
				ingredientStockBatch.sourceType.eq(StockBatchSourceType.INBOUND)
					.and(ingredientStockBatch.expirationDate.isNull())
			).then(1)
			.when(
				ingredientStockBatch.sourceType.eq(StockBatchSourceType.STOCK_ADJUSTMENT)
			).then(2)
			.otherwise(99);

		return queryFactory
			.selectFrom(ingredientStockBatch)
			.join(ingredientStockBatch.ingredient).fetchJoin()
			.where(
				ingredientStockBatch.store.storeId.eq(storeId),
				ingredientStockBatch.ingredient.ingredientId.in(ingredientIds),
				ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
				ingredientStockBatch.remainingQuantity.gt(BigDecimal.ZERO)
			)
			.orderBy(
				ingredientStockBatch.ingredient.ingredientId.asc(),
				sourcePriority.asc(),
				ingredientStockBatch.expirationDate.asc(),
				ingredientStockBatch.createdAt.asc()
			)
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.fetch();
	}

	@Override
	public Optional<BigDecimal> findLatestUnitCostByStoreAndIngredient(Long storeId, Long ingredientId) {
		return Optional.ofNullable(
			queryFactory
				.select(ingredientStockBatch.unitCost)
				.from(ingredientStockBatch)
				.where(
					ingredientStockBatch.store.storeId.eq(storeId),
					ingredientStockBatch.ingredient.ingredientId.eq(ingredientId),
					ingredientStockBatch.unitCost.isNotNull(),
					ingredientStockBatch.unitCost.gt(BigDecimal.ZERO)
				)
				.orderBy(ingredientStockBatch.createdAt.desc())
				.fetchFirst()
		);
	}

	@Override
	public BigDecimal calculateTotalQuantity(Long storeId, Long ingredientId) {
		BigDecimal total = queryFactory
			.select(ingredientStockBatch.remainingQuantity.sum())
			.from(ingredientStockBatch)
			.where(
				ingredientStockBatch.store.storeId.eq(storeId),
				ingredientStockBatch.ingredient.ingredientId.eq(ingredientId),
				ingredientStockBatch.remainingQuantity.gt(BigDecimal.ZERO)
			)
			.fetchOne();

		return total != null ? total : BigDecimal.ZERO;
	}

	@Override
	public Map<Long, BigDecimal> calculateTotalQuantities(Long storeId, List<Long> ingredientIds) {
		return queryFactory
			.select(
				ingredientStockBatch.ingredient.ingredientId,
				ingredientStockBatch.remainingQuantity.sum()
			)
			.from(ingredientStockBatch)
			.where(
				ingredientStockBatch.store.storeId.eq(storeId),
				ingredientStockBatch.ingredient.ingredientId.in(ingredientIds)
			)
			.groupBy(ingredientStockBatch.ingredient.ingredientId)
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				tuple -> tuple.get(0, Long.class),
				tuple -> {
					BigDecimal sum = tuple.get(1, BigDecimal.class);
					return sum != null ? sum : BigDecimal.ZERO;
				}
			));
	}

	@Override
	public Page<StockSummaryResponse> findStockSummaryList(
		Long storeId,
		StockSearchRequest condition,
		Pageable pageable
	) {
		// 1. 데이터 조회 쿼리 (Content)
		List<StockSummaryResponse> content = queryFactory
			.select(Projections.constructor(StockSummaryResponse.class,
				ingredientStockBatch.ingredient.ingredientPublicId,
				ingredientStockBatch.ingredient.name,
				ingredientStockBatch.remainingQuantity.sum(),
				ingredientStockBatch.ingredient.unit,
				ingredientStockBatch.count(),
				ingredientStockBatch.expirationDate.min()
			))
			.from(ingredientStockBatch)
			.where(
				ingredientStockBatch.store.storeId.eq(storeId),
				ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
				ingredientNameContains(condition.ingredientName())
			)
			.groupBy(
				ingredientStockBatch.ingredient.ingredientPublicId,
				ingredientStockBatch.ingredient.name,
				ingredientStockBatch.ingredient.unit
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(ingredientStockBatch.ingredient.name.asc())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(ingredientStockBatch.ingredient.ingredientPublicId.countDistinct())
			.from(ingredientStockBatch)
			.where(
				ingredientStockBatch.store.storeId.eq(storeId),
				ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
				ingredientNameContains(condition.ingredientName())
			);

		// 3. Page 객체 반환 (PageableExecutionUtils 사용 시 count 쿼리 최적화 지원)
		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}

	private BooleanExpression ingredientNameContains(String ingredientName) {
		return StringUtils.hasText(ingredientName)
			? ingredientStockBatch.ingredient.name.contains(ingredientName)
			: null;
	}

	@Override
	public List<IngredientStockBatch> findAvailableBatchesByStore(Long storeId, UUID ingredientPublicIds) {
		if (storeId == null || ingredientPublicIds == null) {
			return Collections.emptyList();
		}

		return queryFactory
			.selectFrom(ingredientStockBatch)
			.join(ingredientStockBatch.ingredient).fetchJoin()
			.where(
				ingredientStockBatch.store.storeId.eq(storeId),
				ingredientStockBatch.ingredient.ingredientPublicId.eq(ingredientPublicIds),
				ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
				ingredientStockBatch.remainingQuantity.gt(BigDecimal.ZERO)
			)
			.orderBy(
				ingredientStockBatch.ingredient.ingredientId.asc(),
				ingredientStockBatch.expirationDate.asc(),
				ingredientStockBatch.createdAt.asc()
			)
			.fetch();
	}

	@Override
	public Page<IngredientStockBatch> findAll(Pageable pageable) {
		// 1. 데이터 조회 (Fetch Join으로 N+1 방지)
		List<IngredientStockBatch> content = queryFactory
			.selectFrom(ingredientStockBatch)
			.join(ingredientStockBatch.ingredient).fetchJoin()
			.join(ingredientStockBatch.store).fetchJoin()
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory
			.select(ingredientStockBatch.count())
			.from(ingredientStockBatch);

		// 3. 페이지 객체 생성 (더 이상 필요 없을 때 count 쿼리 생략 최적화)
		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
	}
    @Override
    public List<IngredientStockTotal> findTotalRemainingByStoreIdAndIngredientIds(Long storeId, List<Long> ingredientIds) {
        QIngredientStockBatch batch = QIngredientStockBatch.ingredientStockBatch;

        return queryFactory
                .select(Projections.constructor(
                        IngredientStockTotal.class,
                        batch.ingredient.ingredientId,
                        batch.remainingQuantity.sum()
                ))
                .from(batch)
                .where(
                        batch.store.storeId.eq(storeId),
                        batch.ingredient.ingredientId.in(ingredientIds)
                )
                .groupBy(batch.ingredient.ingredientId)
                .fetch();
    }
}