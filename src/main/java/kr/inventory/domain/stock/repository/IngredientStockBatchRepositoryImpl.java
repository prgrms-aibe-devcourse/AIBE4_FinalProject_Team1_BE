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

import static kr.inventory.domain.stock.entity.QIngredientStockBatch.ingredientStockBatch;

@RequiredArgsConstructor
public class IngredientStockBatchRepositoryImpl implements IngredientStockBatchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<IngredientStockBatch> findAllAvailableBatchesWithLock(Collection<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return Collections.emptyList();
        }

        return queryFactory
                .selectFrom(ingredientStockBatch)
                .join(ingredientStockBatch.ingredient).fetchJoin()
                .where(
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
    public List<IngredientStockBatch> findAllForAdjustmentWithLock(Long ingredientId) {
        return queryFactory
                .selectFrom(ingredientStockBatch)
                .join(ingredientStockBatch.ingredient).fetchJoin()
                .where(ingredientStockBatch.ingredient.ingredientId.eq(ingredientId))
                .orderBy(
                        ingredientStockBatch.expirationDate.desc().nullsFirst(),
                        ingredientStockBatch.createdAt.desc(),
                        ingredientStockBatch.batchId.desc()
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }
}