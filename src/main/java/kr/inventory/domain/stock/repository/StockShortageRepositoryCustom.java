package kr.inventory.domain.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockShortageRepositoryCustom {
    Page<Long> findDistinctSalesOrderIdsByStoreId(Long storeId, Pageable pageable);
}
