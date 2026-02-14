package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.IngredientStockBatch;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IngredientStockBatchRepositoryCustom {
    List<IngredientStockBatch> findAvailableBatchesByStoreWithLock(Long storeId, Collection<Long> ingredientIds);

    Optional<BigDecimal> findLatestUnitCostByStoreAndIngredient(Long storeId, Long ingredientId);
}
