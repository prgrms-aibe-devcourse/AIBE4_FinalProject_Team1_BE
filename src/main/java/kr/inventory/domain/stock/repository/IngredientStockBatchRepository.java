package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface IngredientStockBatchRepository extends JpaRepository<IngredientStockBatch, Long> {
    List<IngredientStockBatch> findByIngredient_IngredientIdAndStatusAndRemainingQuantityGreaterThanOrderByExpirationDateAscCreatedAtAsc(
            Long ingredientId,
            StockBatchStatus status,
            BigDecimal remainingQuantity
    );
}
