package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientStockBatchRepository extends JpaRepository<IngredientStockBatch, Long>, IngredientStockBatchRepositoryCustom {
}
