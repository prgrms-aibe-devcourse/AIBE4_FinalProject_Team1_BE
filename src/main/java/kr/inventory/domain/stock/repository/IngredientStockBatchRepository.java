package kr.inventory.domain.stock.repository;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientStockBatchRepository extends JpaRepository<IngredientStockBatch, Long>, IngredientStockBatchRepositoryCustom {
    Optional<IngredientStockBatch> findFirstByIngredientOrderByCreatedAtDesc(Ingredient ingredient);
}
