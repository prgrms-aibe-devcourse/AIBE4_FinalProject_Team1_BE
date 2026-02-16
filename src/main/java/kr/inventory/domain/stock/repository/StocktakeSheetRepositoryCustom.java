package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StocktakeSheet;

import java.util.Optional;

public interface StocktakeSheetRepositoryCustom {
    Optional<StocktakeSheet> findByIdAndStoreIdWithLock(Long sheetId, Long storeId);
}
