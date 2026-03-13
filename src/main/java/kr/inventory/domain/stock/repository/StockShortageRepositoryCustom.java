package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.controller.dto.request.StockShortageSearchRequest;
import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface StockShortageRepositoryCustom {
    Page<Long> findDistinctSalesOrderIdsByStoreId(Long storeId, StockShortageSearchRequest searchRequest, Pageable pageable);

    List<StockShortage> findAllBySalesOrderIds(
            List<Long> salesOrderIds,
            StockShortageSearchRequest searchRequest
    );

    Set<Long> findPendingIngredientIds(Long storeId, List<Long> ingredientIds);

    List<StockShortage> findPendingShortages(Long storeId, Collection<Long> ingredientIds, ShortageStatus status);
}
