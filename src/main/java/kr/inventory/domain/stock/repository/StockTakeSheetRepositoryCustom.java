package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.controller.dto.request.StockTakeSheetSearchRequest;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface StockTakeSheetRepositoryCustom {
    Optional<StockTakeSheet> findBySheetPublicIdAndStoreIdWithLock(UUID sheetPublicId, Long storeId);

    Page<StockTakeSheet> searchStockTakeSheets(
            Long storeId,
            StockTakeSheetSearchRequest request,
            Pageable pageable
    );
}
