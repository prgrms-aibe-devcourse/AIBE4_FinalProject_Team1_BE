package kr.inventory.domain.stock.repository;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientStockBatchRepository
	extends JpaRepository<IngredientStockBatch, Long>, IngredientStockBatchRepositoryCustom {
}
