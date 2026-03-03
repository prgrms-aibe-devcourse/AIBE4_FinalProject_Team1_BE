package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.controller.dto.response.StockSummaryResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IngredientStockBatchRepositoryCustom {
	List<IngredientStockBatch> findAvailableBatchesByStoreWithLock(Long storeId, Collection<Long> ingredientIds);

	Optional<BigDecimal> findLatestUnitCostByStoreAndIngredient(Long storeId, Long ingredientId);

	BigDecimal calculateTotalQuantity(Long storeId, Long ingredientId);

	Map<Long, BigDecimal> calculateTotalQuantities(Long storeId, List<Long> ingredientIds);

	List<StockSummaryResponse> findStockSummaryList(Long storeId);

	List<IngredientStockBatch> findAvailableBatchesByStore(Long storeId, Collection<Long> ingredientIds);
}
