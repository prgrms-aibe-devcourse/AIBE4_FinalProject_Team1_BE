package kr.inventory.mcp.repository.stock.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import kr.inventory.mcp.dto.stock.response.GetCurrentStockToolResponse;
import kr.inventory.mcp.repository.stock.StockMcpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static kr.inventory.domain.reference.entity.QIngredient.ingredient;
import static kr.inventory.domain.stock.entity.QIngredientStockBatch.ingredientStockBatch;

@Repository
@RequiredArgsConstructor
public class StockMcpRepositoryImpl implements StockMcpRepository {

    private static final int DEFAULT_LIMIT = 25;

    private final JPAQueryFactory queryFactory;

    @Override
    public long countCurrentStockItems(Long storeId, String keyword) {
        Long count = queryFactory
                .select(ingredient.countDistinct())
                .from(ingredientStockBatch)
                .join(ingredientStockBatch.ingredient, ingredient)
                .where(
                        ingredientStockBatch.store.storeId.eq(storeId),
                        ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
                        ingredientNameContains(keyword)
                )
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<GetCurrentStockToolResponse.Item> findCurrentStockItems(Long storeId, String keyword, int limit) {
        int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;

        return queryFactory
                .select(
                        Projections.constructor(
                                GetCurrentStockToolResponse.Item.class,
                                ingredient.ingredientPublicId,
                                ingredient.name,
                                ingredientStockBatch.remainingQuantity.sum(),
                                ingredient.unit.stringValue()
                        )
                )
                .from(ingredientStockBatch)
                .join(ingredientStockBatch.ingredient, ingredient)
                .where(
                        ingredientStockBatch.store.storeId.eq(storeId),
                        ingredientStockBatch.status.eq(StockBatchStatus.OPEN),
                        ingredientNameContains(keyword)
                )
                .groupBy(
                        ingredient.ingredientPublicId,
                        ingredient.name,
                        ingredient.unit
                )
                .orderBy(ingredient.name.asc())
                .limit(resolvedLimit)
                .fetch();
    }

    private BooleanExpression ingredientNameContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return ingredient.name.containsIgnoreCase(keyword.trim());
    }
}