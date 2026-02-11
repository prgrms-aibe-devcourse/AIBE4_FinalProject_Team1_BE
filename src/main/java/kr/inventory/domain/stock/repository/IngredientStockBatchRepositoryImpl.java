package kr.inventory.domain.stock.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import static kr.inventory.domain.stock.entity.QIngredientStockBatch.ingredientStockBatch;

@RequiredArgsConstructor
public class IngredientStockBatchRepositoryImpl implements IngredientStockBatchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<IngredientStockBatch> findAvailableBatchesWithLock(Long ingredientId) {
        return queryFactory
                .selectFrom(ingredientStockBatch)
                .where(
                        ingredientStockBatch.ingredient.ingredientId.eq(ingredientId),
                        ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
                        ingredientStockBatch.remainingQuantity.gt(BigDecimal.ZERO)
                )
                .orderBy(
                        ingredientStockBatch.expirationDate.asc(),
                        ingredientStockBatch.createdAt.asc()
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }
}