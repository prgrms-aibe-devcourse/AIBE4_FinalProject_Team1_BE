package kr.inventory.domain.stock.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

		return queryFactory
			.selectFrom(ingredientStockBatch)
			.join(ingredientStockBatch.ingredient).fetchJoin()
			.where(
				ingredientStockBatch.storeId.eq(storeId),
				ingredientStockBatch.ingredient.ingredientId.in(ingredientIds),
				ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
				ingredientStockBatch.remainingQuantity.gt(BigDecimal.ZERO)
			)
			.orderBy(
				ingredientStockBatch.ingredient.ingredientId.asc(),
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
					ingredientStockBatch.storeId.eq(storeId),
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
				ingredientStockBatch.storeId.eq(storeId),
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
				ingredientStockBatch.storeId.eq(storeId),
				ingredientStockBatch.ingredient.ingredientId.in(ingredientIds)
			)
			.groupBy(ingredientStockBatch.ingredient.ingredientId)
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				tuple -> tuple.get(ingredientStockBatch.ingredient.ingredientId),
				tuple -> {
					BigDecimal sum = tuple.get(ingredientStockBatch.remainingQuantity.sum());
					return sum != null ? sum : BigDecimal.ZERO;
				}
			));
	}
}