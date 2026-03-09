package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientStockBatchRepository
        extends JpaRepository<IngredientStockBatch, Long>, IngredientStockBatchRepositoryCustom {

    Optional<IngredientStockBatch> findByStore_StoreIdAndBatchPublicId(Long storeId, UUID batchPublicId);

    List<IngredientStockBatch> findByInboundItem(StockInboundItem inboundItem);

}