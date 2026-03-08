package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockShortage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockShortageRepository extends JpaRepository<StockShortage, Long>, StockShortageRepositoryCustom {
    List<StockShortage> findAllByStoreId(Long storeId);

    List<StockShortage> findAllBySalesOrderIdIn(List<Long> salesOrderIds);
}
