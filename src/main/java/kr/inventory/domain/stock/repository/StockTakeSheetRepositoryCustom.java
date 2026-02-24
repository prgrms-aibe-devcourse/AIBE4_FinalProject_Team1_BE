package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockTakeSheet;

import java.util.Optional;

public interface StockTakeSheetRepositoryCustom {
    Optional<StockTakeSheet> findByIdAndStoreIdWithLock(Long sheetId, Long storeId);
}
