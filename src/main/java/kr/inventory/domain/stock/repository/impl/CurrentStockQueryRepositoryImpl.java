package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.ai.stock.tool.enums.StockOverviewSortBy;
import kr.inventory.ai.stock.tool.enums.StockOverviewStatusFilter;
import kr.inventory.domain.reference.entity.QIngredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.stock.entity.QIngredientStockBatch;
import kr.inventory.domain.stock.policy.StockStatusResolver;
import kr.inventory.domain.stock.repository.CurrentStockQueryRepository;
import kr.inventory.domain.stock.service.command.CurrentStockOverviewSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CurrentStockQueryRepositoryImpl implements CurrentStockQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QIngredient ingredient = QIngredient.ingredient;
    private final QIngredientStockBatch batch = QIngredientStockBatch.ingredientStockBatch;

    @Override
    public List<CurrentStockOverviewSummary> findCurrentStockOverview(
            Long storeId,
            String keyword,
            StockOverviewStatusFilter status,
            StockOverviewSortBy sortBy,
            int limit
    ) {
        NumberExpression<BigDecimal> currentQuantityExpr = batch.remainingQuantity.sum().coalesce(BigDecimal.ZERO);

        List<Tuple> rows = queryFactory
                .select(
                        ingredient.ingredientId,
                        ingredient.ingredientPublicId,
                        ingredient.name,
                        ingredient.normalizedName,
                        ingredient.status.stringValue(),
                        ingredient.unit.stringValue(),
                        ingredient.lowStockThreshold,
                        currentQuantityExpr
                )
                .from(ingredient)
                .leftJoin(batch).on(
                        batch.ingredient.eq(ingredient)
                                .and(batch.store.storeId.eq(storeId))
                )
                .where(
                        ingredient.store.storeId.eq(storeId),
                        ingredient.status.eq(IngredientStatus.ACTIVE),
                        containsKeyword(keyword),
                        stockStatusCondition(status, currentQuantityExpr)
                )
                .groupBy(
                        ingredient.ingredientId,
                        ingredient.ingredientPublicId,
                        ingredient.name,
                        ingredient.normalizedName,
                        ingredient.status,
                        ingredient.unit,
                        ingredient.lowStockThreshold
                )
                .orderBy(orderSpecifiers(sortBy, currentQuantityExpr))
                .limit(limit)
                .fetch();

        return rows.stream()
                .map(row -> toSummary(row, currentQuantityExpr))
                .toList();
    }

    private CurrentStockOverviewSummary toSummary(Tuple row, NumberExpression<BigDecimal> currentQuantityExpr) {
        Long ingredientId = row.get(ingredient.ingredientId);
        UUID ingredientPublicId = row.get(ingredient.ingredientPublicId);
        String ingredientName = row.get(ingredient.name);
        String normalizedIngredientName = row.get(ingredient.normalizedName);
        String ingredientStatus = row.get(ingredient.status.stringValue());
        String unit = row.get(ingredient.unit.stringValue());
        BigDecimal lowStockThreshold = row.get(ingredient.lowStockThreshold);
        BigDecimal currentQuantity = row.get(currentQuantityExpr);

        BigDecimal safeCurrentQuantity = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
        String stockStatus = StockStatusResolver.resolve(safeCurrentQuantity, lowStockThreshold);
        boolean belowThreshold = StockStatusResolver.isBelowThreshold(safeCurrentQuantity, lowStockThreshold);

        return new CurrentStockOverviewSummary(
                ingredientId,
                ingredientPublicId,
                ingredientName,
                normalizedIngredientName,
                ingredientStatus,
                unit,
                lowStockThreshold,
                safeCurrentQuantity,
                stockStatus,
                belowThreshold
        );
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return ingredient.name.containsIgnoreCase(keyword)
                .or(ingredient.normalizedName.containsIgnoreCase(keyword));
    }

    private BooleanExpression stockStatusCondition(
            StockOverviewStatusFilter status,
            NumberExpression<BigDecimal> currentQuantityExpr
    ) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case OUT_OF_STOCK -> currentQuantityExpr.loe(BigDecimal.ZERO);
            case LOW_STOCK -> currentQuantityExpr.gt(BigDecimal.ZERO)
                    .and(ingredient.lowStockThreshold.isNotNull())
                    .and(currentQuantityExpr.loe(ingredient.lowStockThreshold));
            case NORMAL -> ingredient.lowStockThreshold.isNull()
                    .or(currentQuantityExpr.gt(ingredient.lowStockThreshold));
        };
    }

    private OrderSpecifier<?>[] orderSpecifiers(
            StockOverviewSortBy sortBy,
            NumberExpression<BigDecimal> currentQuantityExpr
    ) {
        StockOverviewSortBy resolved = sortBy == null ? StockOverviewSortBy.STOCK_ASC : sortBy;

        return switch (resolved) {
            case STOCK_ASC -> new OrderSpecifier[]{
                    currentQuantityExpr.asc(),
                    ingredient.name.asc()
            };
            case STOCK_DESC -> new OrderSpecifier[]{
                    currentQuantityExpr.desc(),
                    ingredient.name.asc()
            };
            case NAME_ASC -> new OrderSpecifier[]{
                    ingredient.name.asc()
            };
            case NAME_DESC -> new OrderSpecifier[]{
                    ingredient.name.desc()
            };
        };
    }
}