package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockShortageRepository extends JpaRepository<StockShortage, Long>, StockShortageRepositoryCustom {
    List<StockShortage> findAllByStoreId(Long storeId);

    List<StockShortage> findAllBySalesOrderIdIn(List<Long> salesOrderIds);

    List<StockShortage> findByStoreIdAndIngredientIdAndStatusOrderByCreatedAtAsc(
            Long storeId,
            Long ingredientId,
            ShortageStatus status
    );

    Optional<StockShortage> findByStockShortagePublicIdAndStoreId(UUID stockShortagePublicId, Long storeId);
}
