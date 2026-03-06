package kr.inventory.domain.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.stock.controller.dto.request.StockLogSearchCondition;
import kr.inventory.domain.stock.controller.dto.response.StockLogResponse;
import kr.inventory.domain.stock.entity.StockLog;

public interface StockLogRepositoryCustom {
	Page<StockLogResponse> searchStockLog(Long storeId, StockLogSearchCondition condition, Pageable pageable);
}
