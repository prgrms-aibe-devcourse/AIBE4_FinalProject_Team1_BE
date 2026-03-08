package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.controller.dto.request.StockShortageSearchRequest;
import kr.inventory.domain.stock.entity.StockShortage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockShortageRepositoryCustom {
    Page<Long> findDistinctSalesOrderIdsByStoreId(Long storeId, StockShortageSearchRequest searchRequest, Pageable pageable);

    List<StockShortage> findAllBySalesOrderIds(
            List<Long> salesOrderIds,
            StockShortageSearchRequest searchRequest
    );
}
