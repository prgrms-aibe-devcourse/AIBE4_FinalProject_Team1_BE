package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockTakeSheet;

import java.util.Optional;
import java.util.UUID;

public interface StockTakeSheetRepositoryCustom {
    Optional<StockTakeSheet> findBySheetPublicIdAndStoreIdWithLock(UUID sheetPublicId, Long storeId);
}
