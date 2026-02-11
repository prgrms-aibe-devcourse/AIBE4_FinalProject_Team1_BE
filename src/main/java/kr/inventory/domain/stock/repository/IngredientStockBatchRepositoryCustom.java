package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.IngredientStockBatch;

import java.util.Collection;
import java.util.List;

public interface IngredientStockBatchRepositoryCustom {
    List<IngredientStockBatch> findAllAvailableBatchesWithLock(Collection<Long> ingredientIds);
}
