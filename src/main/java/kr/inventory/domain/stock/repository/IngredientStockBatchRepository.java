package kr.inventory.domain.stock.repository;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientStockBatchRepository
	extends JpaRepository<IngredientStockBatch, Long>, IngredientStockBatchRepositoryCustom {
	Optional<IngredientStockBatch> findFirstByIngredientOrderByCreatedAtDesc(Ingredient ingredient);

	List<IngredientStockBatch> findByInboundItem_InboundItemId(Long inboundItemId);

	List<IngredientStockBatch> findByStoreIdAndIngredient_IngredientIdAndStatus(
		Long storeId, Long ingredientId, StockBatchStatus status
	);
}
