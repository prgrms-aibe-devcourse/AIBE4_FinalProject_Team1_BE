package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.controller.dto.request.StockSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockSummaryResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.repository.dto.IngredientStockTotalDto;
import kr.inventory.domain.stock.service.command.IngredientStockTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

public interface IngredientStockBatchRepositoryCustom {
	List<IngredientStockBatch> findAvailableBatchesByStoreWithLock(Long storeId, Collection<Long> ingredientIds);

	Optional<BigDecimal> findLatestUnitCostByStoreAndIngredient(Long storeId, Long ingredientId);

	BigDecimal calculateTotalQuantity(Long storeId, Long ingredientId);

	Map<Long, BigDecimal> calculateTotalQuantities(Long storeId, List<Long> ingredientIds);

	Page<StockSummaryResponse> findStockSummaryList(Long storeId, StockSearchRequest condition, Pageable pageable);

	List<IngredientStockBatch> findAvailableBatchesByStore(Long storeId, UUID ingredientPublicIds);

	Page<IngredientStockBatch> findAll(Pageable pageable);
    List<IngredientStockTotal> findTotalRemainingByStoreIdAndIngredientIds(Long storeId, List<Long> ingredientIds);

    List<IngredientStockTotalDto> calculateTotalQuantities(Long storeId, Collection<Long> ingredientIds);
}
