package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockTakeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTakeSheetRepository extends JpaRepository<StockTakeSheet,Long>, StockTakeSheetRepositoryCustom {
    List<StockTakeSheet> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
}