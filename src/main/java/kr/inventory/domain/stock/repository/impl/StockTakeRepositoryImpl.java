package kr.inventory.domain.stock.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.entity.StockTake;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.repository.StockTakeRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static kr.inventory.domain.reference.entity.QIngredient.ingredient;
import static kr.inventory.domain.stock.entity.QStockTake.stockTake;

@RequiredArgsConstructor
public class StockTakeRepositoryImpl implements StockTakeRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockTake> findAllBySheetAndIngredientPublicIdsWithLock(
            StockTakeSheet sheetEntity,
            List<UUID> ingredientPublicIds
    ) {
        if (ingredientPublicIds == null || ingredientPublicIds.isEmpty()) {
            return Collections.emptyList();
        }

        return queryFactory
                .selectFrom(stockTake)
                .join(stockTake.ingredient, ingredient).fetchJoin()
                .where(
                        stockTake.sheet.eq(sheetEntity),
                        ingredient.ingredientPublicId.in(ingredientPublicIds)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
    }
}