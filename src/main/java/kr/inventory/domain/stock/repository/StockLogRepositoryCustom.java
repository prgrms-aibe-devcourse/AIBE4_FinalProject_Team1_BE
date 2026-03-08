package kr.inventory.domain.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.inventory.domain.stock.controller.dto.request.StockLogSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockLogResponse;

public interface StockLogRepositoryCustom {
	Page<StockLogResponse> searchStockLog(Long storeId, StockLogSearchRequest condition, Pageable pageable);
}
